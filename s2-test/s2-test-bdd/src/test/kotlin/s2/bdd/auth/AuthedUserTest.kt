package s2.bdd.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthedUserTest {

    private val user = AuthedUser(
        id = "user-1",
        memberOf = "org-1",
        roles = arrayOf("admin", "writer")
    )

    @Test
    fun `equals should compare id memberOf and roles content`() {
        val same = AuthedUser(id = "user-1", memberOf = "org-1", roles = arrayOf("admin", "writer"))
        val differentRoles = AuthedUser(id = "user-1", memberOf = "org-1", roles = arrayOf("reader"))
        val differentId = AuthedUser(id = "user-2", memberOf = "org-1", roles = arrayOf("admin", "writer"))
        val differentOrg = AuthedUser(id = "user-1", memberOf = null, roles = arrayOf("admin", "writer"))

        assertThat(user).isEqualTo(user)
        assertThat(user).isEqualTo(same)
        assertThat(user.hashCode()).isEqualTo(same.hashCode())
        assertThat(user).isNotEqualTo(differentRoles)
        assertThat(user).isNotEqualTo(differentId)
        assertThat(user).isNotEqualTo(differentOrg)
        assertThat(user).isNotEqualTo("not a user")
    }

    @Test
    fun `hashCode should handle null memberOf`() {
        val withoutOrg = AuthedUser(id = "user-1", memberOf = null, roles = arrayOf("admin"))
        val sameWithoutOrg = AuthedUser(id = "user-1", memberOf = null, roles = arrayOf("admin"))

        assertThat(withoutOrg.hashCode()).isEqualTo(sameWithoutOrg.hashCode())
    }

    @Test
    fun `hasRole should check a single role`() {
        assertThat(user.hasRole("admin")).isTrue()
        assertThat(user.hasRole("reader")).isFalse()
    }

    @Test
    fun `hasRoles should require all roles`() {
        assertThat(user.hasRoles("admin", "writer")).isTrue()
        assertThat(user.hasRoles("admin", "reader")).isFalse()
    }

    @Test
    fun `hasOneOfRoles should require at least one role`() {
        assertThat(user.hasOneOfRoles("reader", "writer")).isTrue()
        assertThat(user.hasOneOfRoles("reader", "auditor")).isFalse()
    }
}
