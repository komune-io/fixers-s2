package s2.sample.multiautomate.infiniteloop

import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2Event
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id

typealias InfiniteLoopId = String

fun infiniteLoopS2(): S2Automate {
    return s2 {
        name = "InfiniteLoopS2"
        init<InfiniteCreateCommand> {
            to = InfiniteLoopState.Running
            role = InfiniteLoopRole.User()
        }
        transaction<InfiniteStepCommand> {
            from = InfiniteLoopState.Running
            to = InfiniteLoopState.Running
            role = InfiniteLoopRole.User()
        }
    }
}

sealed class InfiniteLoopRole : S2Role {
    class User : InfiniteLoopRole()

    override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }
}

open class InfiniteLoopState(
    override val position: Int,
) : S2State {

    object Running : InfiniteLoopState(0)

    override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }
}

interface InfiniteLoopInitCommand : S2InitCommand
interface InfiniteLoopCommand : S2Command<InfiniteLoopId>
interface InfiniteLoopEvent : S2Event<InfiniteLoopState, InfiniteLoopId>, WithS2Id<InfiniteLoopId> {
    override fun s2Id() = id
}

class InfiniteCreateCommand : InfiniteLoopInitCommand

class InfiniteCreatedEvent(
    override val id: InfiniteLoopId,
    override val type: InfiniteLoopState
) : InfiniteLoopEvent
class InfiniteStepCommand(override val id: InfiniteLoopId) : InfiniteLoopCommand
class InfiniteStepEvent(
    override val id: InfiniteLoopId,
    override val type: InfiniteLoopState
) : InfiniteLoopEvent
