package s2.automate.core.appevent.publisher

fun interface AppEventPublisher {
    fun publish(event: Any)
}
