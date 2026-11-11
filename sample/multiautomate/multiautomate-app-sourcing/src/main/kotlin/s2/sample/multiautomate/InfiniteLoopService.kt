package s2.sample.multiautomate

import f2.dsl.fnc.f2Function
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import s2.sample.multiautomate.infiniteloop.InfiniteCreateCommand
import s2.sample.multiautomate.infiniteloop.InfiniteCreatedEvent
import s2.sample.multiautomate.infiniteloop.InfiniteLoopS2Aggregate
import s2.sample.multiautomate.infiniteloop.InfiniteLoopState
import s2.sample.multiautomate.infiniteloop.InfiniteStepCommand
import s2.sample.multiautomate.infiniteloop.InfiniteStepEvent

@Service
class InfiniteLoopService(
    private val aggregate: InfiniteLoopS2Aggregate,
) {

    @Bean
    fun createInfinite() = f2Function<InfiniteCreateCommand, InfiniteCreatedEvent> { command ->
        aggregate.init(command) {
            val id = UUID.randomUUID().toString()
            InfiniteCreatedEvent(id = id, type =  InfiniteLoopState.Running)
        }
    }

    @Bean
    fun stepInfinite() = f2Function<InfiniteStepCommand, InfiniteStepEvent> { command ->
        aggregate.transition(command) { entity ->
            InfiniteStepEvent(id = entity.id, type = InfiniteLoopState.Running)
        }
    }
}
