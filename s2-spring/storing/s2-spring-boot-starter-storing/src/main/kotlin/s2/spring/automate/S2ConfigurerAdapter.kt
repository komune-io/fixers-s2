package s2.spring.automate

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import s2.automate.core.S2AutomateExecutorFlowImpl
import s2.automate.core.S2AutomateExecutorImpl
import s2.automate.core.TransitionStateGuard
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.guard.GuardAdapter
import s2.automate.core.guard.GuardExecutorImpl
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.automate.executor.S2AutomateExecutorSpring
import s2.spring.automate.persister.SpringEventPublisher

abstract class S2ConfigurerAdapter<STATE, ID, ENTITY, out EXECUTER>: InitializingBean where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EXECUTER : S2AutomateExecutorSpring<STATE, ID, ENTITY> {

	@Autowired
	private lateinit var eventPublisher: SpringEventPublisher

	open fun aggregate(): S2AutomateExecutorImpl<STATE, ID, ENTITY, Evt> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val guardExecutor = guardExecutor(publisher)
		val persister = aggregateRepository()
		return S2AutomateExecutorImpl(automateContext, guardExecutor, persister, publisher)
	}
	open fun aggregateFlow(): S2AutomateExecutorFlowImpl<STATE, ID, ENTITY, Evt> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val guardExecutor = guardExecutor(publisher)
		val persister = aggregateRepositoryFlow()
		return S2AutomateExecutorFlowImpl(automateContext, guardExecutor, persister, publisher)
	}

	protected open fun automateContext() = AutomateContext(automate())

	protected open fun guardExecutor(
		automateAppEventPublisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>,
	): GuardExecutorImpl<STATE, ID, ENTITY, Evt, S2Automate> {
		return GuardExecutorImpl(
			guards = guards(),
			publisher = automateAppEventPublisher
		)
	}

	protected open fun automateAppEventPublisher(eventPublisher: SpringEventPublisher)
			: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate> {
		return AutomateEventPublisher(eventPublisher)
	}

	protected open fun guards(): List<GuardAdapter<STATE, ID, ENTITY, Evt, S2Automate>> =
		listOf(TransitionStateGuard())

	override fun afterPropertiesSet() {
		val automateExecutor = aggregate()
		val automateExecutorFlow = aggregateFlow()
		val agg = executor()
		agg.withContext(automateExecutor, automateExecutorFlow, eventPublisher)
	}

	abstract fun aggregateRepository(): AutomatePersister<STATE, ID, ENTITY, Evt, S2Automate>
	abstract fun aggregateRepositoryFlow(): AutomatePersisterFlow<STATE, ID, ENTITY, Evt, S2Automate>
	abstract fun automate(): S2Automate
	abstract fun executor(): EXECUTER
}
