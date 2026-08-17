package s2.dsl.automate.builder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State

class S2SourcingAutomateBuilderTest {

    enum class DraftState(override val position: Int) : S2State {
        Draft(0), Submitted(1), Validated(2)
    }

    object Issuer : S2Role

    data class CreateDraft(val value: String = "") : S2InitCommand
    data class SubmitDraft(override val id: String) : S2Command<String>
    data class UpdateDraft(override val id: String) : S2Command<String>
    data class ValidateDraft(override val id: String) : S2Command<String>
    data class DraftCreated(val id: String) : Evt
    data class DraftSubmitted(val id: String) : Evt
    data class DraftUpdated(val id: String) : Evt
    data class DraftValidated(val id: String) : Evt

    private val automate = s2Sourcing {
        name = "Draft"
        init<CreateDraft, DraftCreated> {
            to = DraftState.Draft
            role = Issuer
        }
        transaction<SubmitDraft, DraftSubmitted> {
            from = DraftState.Draft
            to = DraftState.Submitted
            role = Issuer
            evt = DraftSubmitted::class
        }
        selfTransaction<UpdateDraft, DraftUpdated> {
            states += DraftState.Draft
            states += DraftState.Submitted
            role = Issuer
        }
        node {
            state = DraftState.Submitted
            transaction<ValidateDraft> {
                to = DraftState.Validated
                role = Issuer
                evt = DraftValidated::class
            }
        }
    }

    @Test
    fun `s2Sourcing builder registers every transition flavor`() {
        assertThat(automate.name).isEqualTo("Draft")
        assertThat(automate.version).isNull()
        // init + transaction + selfTransaction(2 states) + node = 5
        assertThat(automate.transitions).hasSize(5)
    }

    @Test
    fun `init defaults its result to the reified event type`() {
        val init = automate.transitions.single { it.from == null }
        assertThat(init.action.name).isEqualTo(CreateDraft::class.simpleName)
        assertThat(init.result?.name).isEqualTo(DraftCreated::class.simpleName)
    }

    @Test
    fun `selfTransaction defaults its result to the reified event type`() {
        val selfs = automate.transitions.filter { it.action.name == UpdateDraft::class.simpleName }
        assertThat(selfs).hasSize(2)
        selfs.forEach {
            assertThat(it.result?.name).isEqualTo(DraftUpdated::class.simpleName)
            assertThat(it.from?.position).isEqualTo(it.to.position)
        }
    }

    @Test
    fun `transaction uses the explicit evt when provided`() {
        val submit = automate.transitions.single { it.action.name == SubmitDraft::class.simpleName }
        assertThat(submit.result?.name).isEqualTo(DraftSubmitted::class.simpleName)
        assertThat(submit.from?.position).isEqualTo(DraftState.Draft.position)
        assertThat(submit.to.position).isEqualTo(DraftState.Submitted.position)
    }

    @Test
    fun `transaction with froms creates one transition per source state`() {
        val multi = s2Sourcing {
            name = "Multi"
            transaction<ValidateDraft, DraftValidated> {
                froms += DraftState.Draft
                froms += DraftState.Submitted
                to = DraftState.Validated
                role = Issuer
            }
        }
        assertThat(multi.transitions).hasSize(2)
    }

    @Test
    fun `transaction defaults its result to the reified event type`() {
        val automate = s2Sourcing {
            name = "Default"
            transaction<ValidateDraft, DraftValidated> {
                from = DraftState.Submitted
                to = DraftState.Validated
                role = Issuer
            }
        }
        val transition = automate.transitions.single()
        assertThat(transition.result?.name).isEqualTo(DraftValidated::class.simpleName)
    }

    @Test
    fun `version set in the builder is carried onto the automate`() {
        val versioned = s2Sourcing {
            name = "Versioned"
            version = "1.2.3"
        }
        assertThat(versioned.version).isEqualTo("1.2.3")
    }

    @Test
    fun `withResultAsAction reflects the sourcing defaults`() {
        val sourcingOnly = s2Sourcing {
            name = "Full"
            init<CreateDraft, DraftCreated> {
                to = DraftState.Draft
                role = Issuer
            }
            selfTransaction<UpdateDraft, DraftUpdated> {
                states += DraftState.Draft
                role = Issuer
            }
        }
        assertThat(sourcingOnly.withResultAsAction).isTrue()
    }
}
