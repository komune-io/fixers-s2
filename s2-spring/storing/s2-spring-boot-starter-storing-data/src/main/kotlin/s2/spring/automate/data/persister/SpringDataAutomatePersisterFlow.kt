package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.repository.CrudRepository
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomatePersisterFlow<STATE, ID: Any, ENTITY, EVENT>(
    private val repository: CrudRepository<ENTITY, ID>,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

    private val loader = AlignedEntityLoader<ID, ENTITY> { ids -> repository.findAllById(ids).toList() }

    override fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID>): Flow<ENTITY?> =
        loader.load(ids)

    override fun persistInit(
        transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> {
        return transitionContexts.map {
            repository.save(it.entity)
            it.event
        }
    }

    override fun persist(
        transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> = flow {
        val contexts = transitionContexts.toList()
        repository.saveAll(contexts.map { it.entity })
        contexts.forEach { emit(it.event) }
    }

}
