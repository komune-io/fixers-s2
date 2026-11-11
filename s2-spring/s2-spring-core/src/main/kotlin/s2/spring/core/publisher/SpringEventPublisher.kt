package s2.spring.core.publisher

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.PayloadApplicationEvent
import org.springframework.stereotype.Component
import s2.automate.core.appevent.publisher.AppEventPublisher

@Component
class SpringEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : AppEventPublisher {
    override fun publish(event: Any) {
        applicationEventPublisher.publishEvent(PayloadApplicationEvent(this, event))
    }
}
