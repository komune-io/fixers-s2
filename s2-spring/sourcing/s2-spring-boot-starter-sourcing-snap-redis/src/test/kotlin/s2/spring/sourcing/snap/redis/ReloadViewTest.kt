package s2.spring.sourcing.snap.redis

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.sourcing.S2AutomateSourcingDecider
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

class ReloadViewTest {

	private data class Model(val id: String)

	private enum class State(override val position: Int) : S2State { Only(0) }

	private class Entity : WithS2State<State> {
		override fun s2State() = State.Only
	}

	private class View(var count: Long = 0, var deleteFailure: Exception? = null) : RedisSnapRepository<Model> {
		var deleted = 0

		override fun all(): Flow<Model> = emptyFlow()
		override suspend fun stored(): List<Model> = emptyList()
		override suspend fun count(): Long = error("emptiness is counted by key")
		override suspend fun storedCount(): Long = count
		override suspend fun get(id: String): Model? = null
		override suspend fun save(entity: Model): Model = entity
		override suspend fun deleteAll(): Int {
			deleted++
			deleteFailure?.let { throw it }
			return count.toInt()
		}
	}

	private class Decider(var failure: Throwable? = null) : S2AutomateSourcingDecider<Entity, State, Any, String> {
		var replayed = 0

		override suspend fun <EVENT_OUT : Any> init(command: S2InitCommand, buildEvent: suspend () -> EVENT_OUT) =
			error("not a command test")

		override suspend fun <EVENT_OUT : Any> transition(command: S2Command<String>, exec: suspend Entity.() -> EVENT_OUT) =
			error("not a command test")

		override suspend fun replayHistory() {
			replayed++
			failure?.let { throw it }
		}
	}

	private val view = View()
	private val decider = Decider()
	private val reload = ReloadView(view, decider)

	@Test
	suspend fun `an empty view is replayed and left as the replay wrote it`() {
		reload.reload()

		assertThat(decider.replayed).isEqualTo(1)
		assertThat(view.deleted).isZero()
	}

	@Test
	suspend fun `a view that holds snapshots is not replayed unless forced`() {
		view.count = 3

		reload.reload()

		assertThat(decider.replayed).isZero()
		assertThat(view.deleted).isZero()
	}

	@Test
	suspend fun `a forced replay empties the view first`() {
		view.count = 3

		reload.reload(forceReload = true)

		assertThat(decider.replayed).isEqualTo(1)
		assertThat(view.deleted).isEqualTo(1)
	}

	@Test
	suspend fun `a replay that fails is emptied, so the next start replays it again, and fails the start`() {
		val failure = IllegalStateException("Redis refused a save")
		decider.failure = failure

		val thrown = runCatching { reload.reload() }.exceptionOrNull()

		assertThat(thrown).isSameAs(failure)
		assertThat(checkNotNull(thrown).suppressed).isEmpty()
		assertThat(view.deleted).isEqualTo(1)
	}

	@Test
	suspend fun `a forced replay that fails is emptied after the failure too`() {
		view.count = 3
		decider.failure = IllegalStateException("Redis refused a save")

		val thrown = runCatching { reload.reload(forceReload = true) }.exceptionOrNull()

		assertThat(thrown).hasMessage("Redis refused a save")
		assertThat(view.deleted).isEqualTo(2)
	}

	@Test
	suspend fun `a cancelled emptying is not taken for a failure of its own`() {
		decider.failure = IllegalStateException("Redis refused a save")
		view.deleteFailure = CancellationException("shutting down")

		val thrown = runCatching { reload.reload() }.exceptionOrNull()

		assertThat(thrown).isInstanceOf(CancellationException::class.java)
	}

	@Test
	suspend fun `a failed replay whose view cannot be emptied stops the start`() {
		val failure = IllegalStateException("Redis refused a save")
		val emptying = IllegalStateException("Redis gone")
		decider.failure = failure
		view.deleteFailure = emptying

		val thrown = runCatching { reload.reload() }.exceptionOrNull()

		assertThat(thrown).isSameAs(failure)
		assertThat(checkNotNull(thrown).suppressed).containsExactly(emptying)
	}

	@Test
	suspend fun `a cancelled replay is not taken for a failure`() {
		decider.failure = CancellationException("shutting down")

		val thrown = runCatching { reload.reload() }.exceptionOrNull()

		assertThat(thrown).isInstanceOf(CancellationException::class.java).hasMessage("shutting down")
		assertThat(view.deleted).isZero()
	}
}
