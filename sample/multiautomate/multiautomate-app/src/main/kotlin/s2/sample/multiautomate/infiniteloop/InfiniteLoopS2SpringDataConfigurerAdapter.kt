package s2.sample.multiautomate.infiniteloop

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopEntity
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopRepository
import s2.spring.automate.executor.S2AutomateExecutorSpring
import s2.spring.automate.data.S2SpringDataConfigurerAdapter

@Configuration
class ApplicationS2SpringDataConfigurerAdapter(
	repository: InfiniteLoopRepository,
) : S2SpringDataConfigurerAdapter<InfiniteLoopState, InfiniteLoopId, InfiniteLoopEntity, InfiniteLoopS2Aggregate>(
	repository) {
	override fun automate() = InfiniteLoopS2()

	@Autowired
	lateinit var infiniteLoopS2Aggregate: InfiniteLoopS2Aggregate

	override fun executor(): InfiniteLoopS2Aggregate = infiniteLoopS2Aggregate
}

@Service
class InfiniteLoopS2Aggregate : S2AutomateExecutorSpring<InfiniteLoopState, InfiniteLoopId, InfiniteLoopEntity>()
