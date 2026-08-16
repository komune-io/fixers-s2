package s2.sample.multiautomate.infiniteloop

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopEntity
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopRepository
import s2.spring.automate.data.S2SpringDataReactiveConfigurerAdapter
import s2.spring.automate.executor.S2AutomateExecutorSpring

@Configuration
class InfiniteLoopS2SpringDataConfigurerAdapter(
	repository: InfiniteLoopRepository,
) : S2SpringDataReactiveConfigurerAdapter<
		InfiniteLoopState,
		InfiniteLoopId,
		InfiniteLoopEntity,
		InfiniteLoopS2Aggregate
		>(
	repository) {
	override fun automate() = infiniteLoopS2()

	@Autowired
	lateinit var infiniteLoopS2Aggregate: InfiniteLoopS2Aggregate

	override fun executor(): InfiniteLoopS2Aggregate = infiniteLoopS2Aggregate
}

@Service
class InfiniteLoopS2Aggregate : S2AutomateExecutorSpring<InfiniteLoopState, InfiniteLoopId, InfiniteLoopEntity>()
