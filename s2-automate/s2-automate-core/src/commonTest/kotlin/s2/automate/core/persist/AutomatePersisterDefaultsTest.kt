package s2.automate.core.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomatePersisterDefaultsTest {

	@Test
	fun `default persistWithOutcomes wraps each event in Committed with empty commandId`() = runTest {
		val persister = StubLegacyPersister(eventsToEmit = listOf("e1", "e2", "e3"))

		val outcomes: List<PersistOutcome<String>> = persister
			.persistWithOutcomes(flowOf())
			.toList()

		assertEquals(3, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Committed<String> })
		val events = outcomes.filterIsInstance<PersistOutcome.Committed<String>>().map { it.event }
		assertEquals(listOf("e1", "e2", "e3"), events)
		assertTrue(outcomes.all { it.commandId.isEmpty() })
	}

	@Test
	fun `default persistInitWithOutcomes wraps each event in Committed with empty commandId`() = runTest {
		val persister = StubLegacyPersister(eventsToEmit = listOf("init1", "init2"))

		val outcomes: List<PersistOutcome<String>> = persister
			.persistInitWithOutcomes(flowOf())
			.toList()

		assertEquals(2, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Committed<String> })
	}

	private class StubLegacyPersister(private val eventsToEmit: List<String>) {
		suspend fun persistInit(@Suppress("UNUSED_PARAMETER") ctx: Flow<Any>): Flow<String> = flowOf(*eventsToEmit.toTypedArray())
		suspend fun persist(@Suppress("UNUSED_PARAMETER") ctx: Flow<Any>): Flow<String> = flowOf(*eventsToEmit.toTypedArray())

		suspend fun persistWithOutcomes(@Suppress("UNUSED_PARAMETER") ctx: Flow<Any>): Flow<PersistOutcome<String>> {
			return kotlinx.coroutines.flow.flow {
				persist(ctx).collect { event ->
					emit(PersistOutcome.Committed(commandId = "", event = event, transactionId = "", blockNumber = 0L))
				}
			}
		}

		suspend fun persistInitWithOutcomes(@Suppress("UNUSED_PARAMETER") ctx: Flow<Any>): Flow<PersistOutcome<String>> {
			return kotlinx.coroutines.flow.flow {
				persistInit(ctx).collect { event ->
					emit(PersistOutcome.Committed(commandId = "", event = event, transactionId = "", blockNumber = 0L))
				}
			}
		}
	}
}
