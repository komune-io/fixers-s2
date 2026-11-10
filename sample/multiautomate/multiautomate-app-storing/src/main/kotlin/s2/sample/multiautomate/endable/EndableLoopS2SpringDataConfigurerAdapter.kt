package s2.sample.multiautomate.endable

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.endable.entity.EndableLoopEntity
import s2.sample.multiautomate.endable.entity.EndableLoopRepository
import s2.spring.automate.data.S2SpringDataReactiveConfigurerAdapter
import s2.spring.automate.executor.S2AutomateExecutorSpring

@Configuration
class EndableLoopS2SpringDataConfigurerAdapter(
    repository: EndableLoopRepository,
) : S2SpringDataReactiveConfigurerAdapter<
        EndableLoopState, EndableLoopId, EndableLoopEntity, EndableLoopS2Aggregate
        >(repository) {
    override fun automate() = endableLoopS2()

    @Autowired
    lateinit var endableLoopS2Aggregate: EndableLoopS2Aggregate

    override fun executor(): EndableLoopS2Aggregate = endableLoopS2Aggregate
}

@Service
class EndableLoopS2Aggregate : S2AutomateExecutorSpring<EndableLoopState, EndableLoopId, EndableLoopEntity>()
