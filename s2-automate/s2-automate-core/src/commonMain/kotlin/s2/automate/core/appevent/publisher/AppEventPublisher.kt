package s2.automate.core.appevent.publisher

import s2.automate.core.appevent.AutomatePersistFailure

interface AppEventPublisher {
	fun <EVENT> publish(event: EVENT  & Any)

	fun automatePersistFailure(event: AutomatePersistFailure) {
		publish(event)
	}
}
