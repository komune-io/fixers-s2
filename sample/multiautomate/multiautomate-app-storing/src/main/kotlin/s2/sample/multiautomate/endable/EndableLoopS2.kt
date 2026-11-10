package s2.sample.multiautomate.endable

import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2Event
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2

typealias EndableLoopId = String

fun endableLoopS2(): S2Automate = s2 {
    name = "EndableLoopS2"
    init<EndableCreateCommand> {
        to = EndableLoopState.Running
        role = EndableLoopRole.User()
    }
    transaction<EndableStepCommand> {
        from = EndableLoopState.Running
        to = EndableLoopState.Running
        role = EndableLoopRole.User()
    }
    transaction<EndableEndCommand> {
        from = EndableLoopState.Running
        to = EndableLoopState.Ended
        role = EndableLoopRole.User()
    }
}

sealed class EndableLoopRole : S2Role {
    class User : EndableLoopRole()

    override fun toString(): String = this::class.simpleName ?: super.toString()
}

open class EndableLoopState(
    override val position: Int,
) : S2State {
    object Running : EndableLoopState(0)
    object Ended : EndableLoopState(1)

    override fun toString(): String = this::class.simpleName ?: super.toString()
}

interface EndableLoopInitCommand : S2InitCommand
interface EndableLoopCommand : S2Command<EndableLoopId>
interface EndableLoopEvent : S2Event<EndableLoopState, EndableLoopId>

data class EndableCreateCommand(
    val tt: String,
) : EndableLoopInitCommand

data class EndableCreatedEvent(
    override val id: EndableLoopId,
    override val type: EndableLoopState,
    val tt: String,
) : EndableLoopEvent

data class EndableStepCommand(override val id: EndableLoopId) : EndableLoopCommand
data class EndableStepEvent(
    override val id: EndableLoopId,
    override val type: EndableLoopState,
) : EndableLoopEvent

data class EndableEndCommand(override val id: EndableLoopId) : EndableLoopCommand
data class EndableEndedEvent(
    override val id: EndableLoopId,
    override val type: EndableLoopState,
) : EndableLoopEvent
