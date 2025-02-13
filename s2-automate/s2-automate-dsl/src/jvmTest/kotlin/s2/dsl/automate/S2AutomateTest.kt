package s2.dsl.automate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import s2.dsl.automate.builder.s2Sourcing

class S2AutomateTest {


    @Test
    fun `build automate`() {
        val s2CatalogueDraft = s2Sourcing {
            name = "CatalogueDraft"
            init<CatalogueDraftCreateCommand, CatalogueDraftCreatedEvent> {
                to = CatalogueDraftState.DRAFT
                role = CatalogueDraftRole.Issuer
            }
            transaction<CatalogueDraftSubmitCommand, CatalogueDraftSubmittedEvent> {
                from = CatalogueDraftState.DRAFT
                to = CatalogueDraftState.SUBMITTED
                role = CatalogueDraftRole.Issuer
            }
            transaction<CatalogueDraftRequestUpdateCommand, CatalogueDraftRequestedUpdateEvent> {
                froms += CatalogueDraftState.SUBMITTED
                froms += CatalogueDraftState.REJECTED
                to = CatalogueDraftState.UPDATE_REQUESTED
                role = CatalogueDraftRole.Issuer
            }
            transaction<CatalogueDraftRejectCommand, CatalogueDraftRejectedEvent> {
                froms += CatalogueDraftState.DRAFT
                froms += CatalogueDraftState.SUBMITTED
                froms += CatalogueDraftState.UPDATE_REQUESTED
                to = CatalogueDraftState.REJECTED
                role = CatalogueDraftRole.Issuer
            }
            transaction<CatalogueDraftValidateCommand, CatalogueDraftValidatedEvent> {
                froms += CatalogueDraftState.DRAFT
                froms += CatalogueDraftState.SUBMITTED
                froms += CatalogueDraftState.UPDATE_REQUESTED
                froms += CatalogueDraftState.REJECTED
                to = CatalogueDraftState.VALIDATED
                role = CatalogueDraftRole.Issuer
            }
        }
        val draft = s2CatalogueDraft.getAvailableTransitions(CatalogueDraftState.DRAFT).map { it.action.name }
        Assertions.assertThat(draft).hasSize(3).containsExactlyInAnyOrder(
            CatalogueDraftSubmitCommand::class.simpleName,
            CatalogueDraftRejectCommand::class.simpleName,
            CatalogueDraftValidateCommand::class.simpleName
        )

    }
}

enum class CatalogueDraftState(override val position: Int): S2State {
    DRAFT(0),
    SUBMITTED(1),
    UPDATE_REQUESTED(2),
    VALIDATED(3),
    REJECTED(4)
}

enum class CatalogueDraftRole(val value: String): S2Role {
    Issuer("Issuer");
    override fun toString() = value
}

interface CatalogueDraftCommand: S2InitCommand
interface CatalogueDraftEvent: S2Event<CatalogueDraftState, String>

interface CatalogueDraftCreateCommand: CatalogueDraftCommand
interface CatalogueDraftCreatedEvent: S2Event<CatalogueDraftState, String>

interface CatalogueDraftSubmitCommand: CatalogueDraftCommand
interface CatalogueDraftSubmittedEvent: S2Event<CatalogueDraftState, String>

interface CatalogueDraftRequestUpdateCommand: CatalogueDraftCommand
interface CatalogueDraftRequestedUpdateEvent: S2Event<CatalogueDraftState, String>

interface CatalogueDraftRejectCommand: CatalogueDraftCommand
interface CatalogueDraftRejectedEvent: S2Event<CatalogueDraftState, String>

interface CatalogueDraftValidateCommand: CatalogueDraftCommand
interface CatalogueDraftValidatedEvent: S2Event<CatalogueDraftState, String>
