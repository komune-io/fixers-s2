package s2.spring.automate.sourcing

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import s2.automate.core.TransitionStateGuard
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.guard.Guard
import s2.automate.core.guard.GuardExecutorImpl
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.snap.SnapLoader
import s2.sourcing.dsl.snap.SnapRepository
import s2.sourcing.dsl.view.View
import s2.sourcing.dsl.view.ViewLoader
import s2.spring.automate.persister.SpringEventPublisher
import kotlin.reflect.KClass
import s2.automate.storing.AutomateStoringExecutorImpl

abstract class S2AutomateDeciderSpringAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(
	val executor: EXECUTOR,
	val view: View<EVENT, ENTITY>,
	val snapRepository: SnapRepository<ENTITY, ID>? = null
): InitializingBean where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID>,
EXECUTOR : S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> {

	@Autowired
	lateinit var eventPublisher: SpringEventPublisher

	open fun snapLoader(): Loader<EVENT, ENTITY, ID> {
		val viewLoader = ViewLoader(eventStore(), view)
		return snapRepository?.let { repo ->
			SnapLoader(repo, viewLoader)
		} ?: viewLoader
	}

	open fun viewLoader(): Loader<EVENT, ENTITY, ID> {
		return ViewLoader(eventStore(), view)
	}


	open fun aggregate(
		projectionBuilder: Loader<EVENT, ENTITY, ID>
	): AutomateStoringExecutorImpl<STATE, ID, ENTITY, EVENT> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val guardExecutor = guardExecutor(publisher)
		val eventStore = eventStore()
		return AutomateStoringExecutorImpl<STATE, ID, ENTITY, EVENT>(
			automateContext = automateContext,
			guardExecutor = guardExecutor,
			persister = AutomateStoringPersister(
				projectionLoader = projectionBuilder,
				eventStore = eventStore,
			),
			publisher = publisher
		).also {
			executor.withContext(it, eventPublisher, projectionBuilder)
		}
	}

	protected open fun automateContext() = AutomateContext(automate())

	protected open fun guardExecutor(
		automateAppEventPublisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>,
	): GuardExecutorImpl<STATE, ID, ENTITY, EVENT, S2Automate> {
		return GuardExecutorImpl(
			guards = guards(),
			publisher = automateAppEventPublisher
		)
	}

	protected open fun automateAppEventPublisher(eventPublisher: SpringEventPublisher)
			: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate> {
		return AutomateEventPublisher(eventPublisher)
	}

	protected open fun guards(): List<Guard<STATE, ID, ENTITY, EVENT, S2Automate>> = listOf(
		TransitionStateGuard()
	)

	abstract fun automate(): S2Automate
	abstract fun eventStore(): EventRepository<EVENT, ID>
	abstract fun entityType(): KClass<EVENT>
	override fun afterPropertiesSet() {
		val loader = snapLoader()
		aggregate(loader)
	}
}
