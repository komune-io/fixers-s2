package s2.bdd.auth

typealias OrganizationId = String
typealias UserId = String

interface AuthedUserDTO {
    val id: UserId
    val memberOf: OrganizationId?
    val roles: Array<String>
}

data class AuthedUser(
    override val id: UserId,
    override val memberOf: OrganizationId?,
    override val roles: Array<String>
): AuthedUserDTO {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuthedUser) return false
        return id == other.id && memberOf == other.memberOf && roles.contentEquals(other.roles)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (memberOf?.hashCode() ?: 0)
        result = 31 * result + roles.contentHashCode()
        return result
    }
}

fun AuthedUserDTO.hasRole(role: String) = role in roles
fun AuthedUserDTO.hasRole(role: Role) = role.value in roles
fun AuthedUserDTO.hasRoles(vararg roles: String) = roles.all(this.roles::contains)
fun AuthedUserDTO.hasRoles(vararg roles: Role) = roles.map(Role::value).all(this.roles::contains)
fun AuthedUserDTO.hasOneOfRoles(vararg roles: Role) = roles.map(Role::value).any(this.roles::contains)
fun AuthedUserDTO.hasOneOfRoles(vararg roles: String) = roles.any(this.roles::contains)
