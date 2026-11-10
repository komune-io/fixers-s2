package s2.sample.multiautomate

import f2.dsl.fnc.f2Function
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import s2.sample.multiautomate.endable.EndableLoopState
import s2.sample.multiautomate.infiniteloop.InfiniteCreateCommand
import s2.sample.multiautomate.infiniteloop.InfiniteCreatedEvent
import s2.sample.multiautomate.infiniteloop.InfiniteLoopS2Aggregate
import s2.sample.multiautomate.infiniteloop.InfiniteLoopState
import s2.sample.multiautomate.infiniteloop.InfiniteStepCommand
import s2.sample.multiautomate.infiniteloop.InfiniteStepEvent
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopEntity

@Service
class InfiniteLoopService(
    private val aggregate: InfiniteLoopS2Aggregate,
) {

    @Bean
    fun createInfinite() = f2Function<InfiniteCreateCommand, InfiniteCreatedEvent> { command ->
        aggregate.createWithEvent(command) {
            val id = UUID.randomUUID().toString()
            InfiniteLoopEntity(id, 0, EndableLoopState.Running.position) to InfiniteCreatedEvent(
                id = id,
                type = InfiniteLoopState.Running,
            )
        }
    }

    @Bean
    fun stepInfinite() = f2Function<InfiniteStepCommand, InfiniteStepEvent> { command ->
        aggregate.doTransition(command) {
            this.copy(step = this.step + 1) to InfiniteStepEvent(id = command.id, type = this.s2State())
        }
    }
}
