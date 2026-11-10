package s2.sample.multiautomate.endable

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.endable.entity.EndableLoopEntity
import s2.sample.multiautomate.endable.entity.EndableLoopRepository
import s2.spring.automate.data.S2SpringDataReactiveConfigurerAdapter
import s2.spring.automate.executor.S2AutomateExecutorSpring

@Configuration
class EndableLoopS2SpringDataConfigurerAdapter(
    repository: EndableLoopRepository,
    private val endableLoopS2Aggregate: EndableLoopS2Aggregate,
) : S2SpringDataReactiveConfigurerAdapter<
        EndableLoopState, EndableLoopId, EndableLoopEntity, EndableLoopS2Aggregate
        >(repository) {
    override fun automate() = endableLoopS2()

    override fun executor(): EndableLoopS2Aggregate = endableLoopS2Aggregate
}

@Service
class EndableLoopS2Aggregate : S2AutomateExecutorSpring<EndableLoopState, EndableLoopId, EndableLoopEntity>()
