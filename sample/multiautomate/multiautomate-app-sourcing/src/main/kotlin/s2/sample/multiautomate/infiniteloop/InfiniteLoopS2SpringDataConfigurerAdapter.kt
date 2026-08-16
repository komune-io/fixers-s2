package s2.sample.multiautomate.infiniteloop

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.multiautomate.infiniteloop.entity.InfiniteLoopEntity
import s2.sourcing.dsl.view.View
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.sourcing.data.S2SourcingSpringDataAdapter

@Configuration
class InfiniteLoopS2SpringDataConfigurerAdapter(
	infiniteLoopS2Aggregate: InfiniteLoopS2Aggregate,
	infiniteLoopS2View: InfiniteLoopS2View
) : S2SourcingSpringDataAdapter<
		InfiniteLoopEntity, InfiniteLoopState, InfiniteLoopEvent, InfiniteLoopId, InfiniteLoopS2Aggregate
		>(infiniteLoopS2Aggregate, infiniteLoopS2View) {

	override fun automate() = infiniteLoopS2()
	override fun entityType() = InfiniteLoopEvent::class
}

@Service
class InfiniteLoopS2Aggregate
	: S2AutomateDeciderSpring<InfiniteLoopEntity, InfiniteLoopState, InfiniteLoopEvent, InfiniteLoopId>()

@Service
class InfiniteLoopS2View : View<InfiniteLoopEvent, InfiniteLoopEntity> {
	override suspend fun evolve(event: InfiniteLoopEvent, model: InfiniteLoopEntity?): InfiniteLoopEntity? {
		TODO("Not yet implemented")
	}

}
