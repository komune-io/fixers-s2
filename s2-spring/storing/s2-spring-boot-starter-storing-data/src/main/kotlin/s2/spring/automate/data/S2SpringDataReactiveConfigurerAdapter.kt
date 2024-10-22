package s2.spring.automate.data

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.automate.S2ConfigurerAdapter
import s2.spring.automate.data.persister.SpringDataAutomateReactivePersister
import s2.spring.automate.data.persister.SpringDataAutomateReactivePersisterFlow
import s2.spring.automate.executor.S2AutomateExecutorSpring

abstract class S2SpringDataReactiveConfigurerAdapter<STATE, ID, ENTITY, AGGREGATE>(
	private val aggregateRepository: ReactiveCrudRepository<ENTITY, ID>,
) : S2ConfigurerAdapter<STATE, ID, ENTITY, AGGREGATE>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
AGGREGATE : S2AutomateExecutorSpring<STATE, ID, ENTITY> {

	override fun aggregateRepository(): AutomatePersister<STATE, ID, ENTITY, Evt, S2Automate> {
		return SpringDataAutomateReactivePersister(
			aggregateRepository
		)
	}
	override fun aggregateRepositoryFlow(): AutomatePersisterFlow<STATE, ID, ENTITY, Evt, S2Automate> {
		return SpringDataAutomateReactivePersisterFlow(
			aggregateRepository
		)
	}
}
