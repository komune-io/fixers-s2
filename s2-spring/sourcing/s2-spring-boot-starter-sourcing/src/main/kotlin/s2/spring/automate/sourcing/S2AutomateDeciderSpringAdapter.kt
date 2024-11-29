package s2.spring.automate.sourcing

import java.util.UUID
import kotlin.reflect.KClass
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.GenericApplicationContext
import s2.automate.core.engine.S2AutomateEngineImpl
import s2.automate.core.storing.snap.RetryTaskChannel
import s2.automate.core.storing.snap.SnapPersister
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
import s2.spring.automate.sourcing.persist.S2AutomateSourcingPersister
import s2.spring.core.S2SpringAdapterBase
import s2.spring.core.publisher.SpringEventPublisher

abstract class S2AutomateDeciderSpringAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(
	val executor: EXECUTOR,
	val view: View<EVENT, ENTITY>,
	val snapRepository: SnapRepository<ENTITY, ID>? = null,
): InitializingBean, ApplicationContextAware, S2SpringAdapterBase<ENTITY, STATE, EVENT, ID>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID>,
EXECUTOR : S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> {

	private lateinit var applicationContext: ApplicationContext

	@Autowired
	lateinit var eventPublisher: SpringEventPublisher

	@Autowired
	lateinit var retryTaskChannel: RetryTaskChannel

	override fun setApplicationContext(applicationContext: ApplicationContext) {
		this.applicationContext = applicationContext
	}

	open fun aggregate(
		projectionBuilder: Loader<EVENT, ENTITY, ID>,
		eventStore: EventRepository<EVENT, ID>,
	): S2AutomateEngineImpl<STATE, ID, ENTITY, EVENT> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val snapPersister = SnapPersister(
			projectionBuilder,
			snapRepository,
			retryTaskChannel.takeIf { preventOptimisticLocking() }
		)
		val guardExecutor = guardExecutor(publisher)
		return S2AutomateEngineImpl(
			automateContext = automateContext,
			guardExecutor = guardExecutor,
			persister = S2AutomateSourcingPersister(
				projectionLoader = projectionBuilder,
				eventStore = eventStore,
				snapPersister = snapPersister,
			),
			publisher = publisher
		).also {
			executor.withContext(it, eventPublisher, projectionBuilder, eventStore)
		}
	}

	abstract override fun automate(): S2Automate
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
