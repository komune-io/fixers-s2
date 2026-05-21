package s2.automate.core.appevent.publisher

interface AppEventPublisher {
	fun <EVENT> publish(event: EVENT & Any)
}
