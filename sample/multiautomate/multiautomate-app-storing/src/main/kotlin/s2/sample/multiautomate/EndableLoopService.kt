package s2.sample.multiautomate

import f2.dsl.fnc.f2Function
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import s2.sample.multiautomate.endable.EndableCreateCommand
import s2.sample.multiautomate.endable.EndableCreatedEvent
import s2.sample.multiautomate.endable.EndableEndCommand
import s2.sample.multiautomate.endable.EndableEndedEvent
import s2.sample.multiautomate.endable.EndableLoopS2Aggregate
import s2.sample.multiautomate.endable.EndableLoopState
import s2.sample.multiautomate.endable.EndableStepCommand
import s2.sample.multiautomate.endable.EndableStepEvent
import s2.sample.multiautomate.endable.entity.EndableLoopEntity

@Service
class EndableLoopService(
    private val endableLoopS2Aggregate: EndableLoopS2Aggregate,
) {

    @Bean
    fun createEndable() = f2Function<EndableCreateCommand, EndableCreatedEvent> { command ->
        endableLoopS2Aggregate.createWithEvent(command) {
            val id = UUID.randomUUID().toString()
            EndableLoopEntity(id = id, step = 0, state = EndableLoopState.Running.position) to EndableCreatedEvent(
                id = id,
                type = EndableLoopState.Running,
                tt = command.tt
            )
        }
    }

    @Bean
    fun stepEndable() = f2Function<EndableStepCommand, EndableStepEvent> { command ->
        endableLoopS2Aggregate.doTransition(command) {
            val stepped = copy(step = step + 1)
            stepped to EndableStepEvent(id = stepped.id, type = stepped.s2State())
        }
    }

    @Bean
    fun endEndable() = f2Function<EndableEndCommand, EndableEndedEvent> { command ->
        endableLoopS2Aggregate.doTransition(command) {
            val ended = copy(step = step + 1, state = EndableLoopState.Ended.position)
            ended to EndableEndedEvent(id = ended.id, type = ended.s2State())
        }
    }
}
