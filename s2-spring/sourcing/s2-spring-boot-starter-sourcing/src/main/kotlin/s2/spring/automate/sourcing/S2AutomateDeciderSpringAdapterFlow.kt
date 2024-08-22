package s2.spring.automate.sourcing

import java.util.UUID
import kotlin.reflect.KClass
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.GenericApplicationContext
import s2.automate.core.S2AutomateExecutorFlowImpl
import s2.automate.core.S2AutomateExecutorImpl
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
import s2.spring.automate.sourcing.persist.AutomateSourcingPersister
import s2.spring.automate.sourcing.persist.AutomateSourcingPersisterFlow

abstract class S2AutomateDeciderSpringAdapterFlow<ENTITY, STATE, EVENT, ID, EXECUTOR>(
	val executor: EXECUTOR,
	val view: View<EVENT, ENTITY>,
	val snapRepository: SnapRepository<ENTITY, ID>? = null,
): InitializingBean, ApplicationContextAware where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID>,
EXECUTOR : S2AutomateDeciderSpringFlow<ENTITY, STATE, EVENT, ID> {

	private lateinit var applicationContext: ApplicationContext

	@Autowired
	lateinit var eventPublisher: SpringEventPublisher

	@Autowired
	lateinit var automateSourcingPersisterSnapChannel: AutomateSourcingPersisterSnapChannel

	override fun setApplicationContext(applicationContext: ApplicationContext) {
		this.applicationContext = applicationContext
	}

	open fun aggregate(
		projectionBuilder: Loader<EVENT, ENTITY, ID>,
		eventStore: EventRepository<EVENT, ID>,
	): S2AutomateExecutorFlowImpl<STATE, ID, ENTITY, EVENT> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val guardExecutor = guardExecutor(publisher)
		return S2AutomateExecutorFlowImpl(
			automateContext = automateContext,
			guardExecutor = guardExecutor,
			persister = AutomateSourcingPersisterFlow(
				projectionLoader = projectionBuilder,
				eventStore = eventStore,
				snapRepository = snapRepository,
				automateSourcingPersisterSnapChannel = automateSourcingPersisterSnapChannel.takeIf { preventOptimisticLocking() }
			),
			publisher = publisher
		).also {
			executor.withContext(it, eventPublisher, projectionBuilder, eventStore)
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
	open fun preventOptimisticLocking(): Boolean = false

	override fun afterPropertiesSet() {
		val store = eventStore()
		val viewLoader = viewLoader(store)
		val snapLoader = snapLoader(viewLoader)

		val beanFactory = (applicationContext as GenericApplicationContext).beanFactory
		beanFactory.registerSingleton(store.toString(), store)
		beanFactory.registerSingleton("snapLoader-${UUID.randomUUID()}", snapLoader)

		aggregate(snapLoader, store)
	}


	protected open fun snapLoader(viewLoader: ViewLoader<EVENT, ENTITY, ID>): Loader<EVENT, ENTITY, ID> {
		return snapRepository?.let { repo ->
			SnapLoader(repo, viewLoader)
		} ?: viewLoader
	}

	protected open fun viewLoader(eventStore: EventRepository<EVENT, ID>): ViewLoader<EVENT, ENTITY, ID> {
		return ViewLoader(eventStore, view)
	}
}
