package s2.sample.multiautomate.endable

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.endable.entity.EndableLoopEntity
import s2.sourcing.dsl.view.View
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.sourcing.data.S2SourcingSpringDataAdapter

@Configuration
class EndableLoopS2SpringDataConfigurerAdapter(
    endableLoopS2Aggregate: EndableLoopS2Aggregate,
    endableLoopS2View: EndableLoopS2View
) : S2SourcingSpringDataAdapter<
        EndableLoopEntity, EndableLoopState, EndableLoopEvent, EndableLoopId, EndableLoopS2Aggregate
>(endableLoopS2Aggregate, endableLoopS2View) {

    override fun automate() = endableLoopS2()
    override fun entityType() = EndableLoopEvent::class
}

@Service
class EndableLoopS2Aggregate
    : S2AutomateDeciderSpring<EndableLoopEntity, EndableLoopState, EndableLoopEvent, EndableLoopId>()

@Service
class EndableLoopS2View : View<EndableLoopEvent, EndableLoopEntity> {
    override suspend fun evolve(event: EndableLoopEvent, model: EndableLoopEntity?): EndableLoopEntity? =
        when (event) {
            is EndableCreatedEvent -> EndableLoopEntity(id = event.id, step = 0, state = event.type.position)
            is EndableStepEvent -> model?.copy(step = model.step + 1)
            is EndableEndedEvent -> model?.copy(step = model.step + 1, state = EndableLoopState.Ended.position)
            else -> model
        }
}
