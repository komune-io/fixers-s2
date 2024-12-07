package s2.spring.automate

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import s2.automate.core.engine.S2AutomateEngineImpl
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.automate.executor.S2AutomateExecutorSpring
import s2.spring.core.S2SpringAdapterBase
import s2.spring.core.publisher.SpringEventPublisher

abstract class S2ConfigurerAdapter<STATE, ID, ENTITY, out EXECUTER>
	: InitializingBean, S2SpringAdapterBase<ENTITY, STATE, Evt, ID>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EXECUTER : S2AutomateExecutorSpring<STATE, ID, ENTITY> {


	@Autowired
	private lateinit var eventPublisher: SpringEventPublisher

	open fun aggregate(): S2AutomateEngineImpl<STATE, ID, ENTITY, Evt> {
		val automateContext = automateContext()
		val publisher = automateAppEventPublisher(eventPublisher)
		val guardExecutor = guardExecutor(publisher)
		val persister = aggregateRepository()
		return S2AutomateEngineImpl(automateContext, guardExecutor, persister, publisher)
	}

	override fun afterPropertiesSet() {
		val automateExecutor = aggregate()
		val agg = executor()
		agg.withContext(automateExecutor, eventPublisher)
	}

	abstract override fun automate(): S2Automate
	abstract fun aggregateRepository(): AutomatePersister<STATE, ID, ENTITY, Evt, S2Automate>
	abstract fun executor(): EXECUTER
}
