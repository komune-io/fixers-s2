package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * A tag clause is `@field:{value}`; inside the braces RediSearch reads punctuation and spaces as query syntax
 * unless each one carries a backslash. A typed text becomes word prefixes.
 */
class QueryTextTest {

    @Test
    fun `a plain value is written as it is`() {
        assertThat(tagClause("status", "Deleted")).isEqualTo("@status:{Deleted}")
    }

    @Test
    fun `a value carrying query syntax stays inside its braces`() {
        assertThat(tagClause("code", "38} | @status:{Deleted"))
            .isEqualTo("""@code:{38\}\ \|\ \@status\:\{Deleted}""")
    }

    @Test
    fun `every punctuation mark of a value is escaped`() {
        assertThat(tagClause("f", "a-b.c(d)*e,f'g\"h\\i"))
            .isEqualTo("""@f:{a\-b\.c\(d\)\*e\,f\'g\"h\\i}""")
    }

    @Test
    fun `letters of any alphabet, digits and underscores are not escaped`() {
        assertThat(tagClause("city", "Saint_Égrève2")).isEqualTo("@city:{Saint_Égrève2}")
    }

    @Test
    fun `a union keeps its separators and escapes each member`() {
        assertThat(tagClause("id", listOf("a-1", "b|c"))).isEqualTo("""@id:{a\-1|b\|c}""")
    }

    @Test
    fun `each word becomes a prefix`() {
        assertThat("Saint Égrève".addWildcard()).isEqualTo("Saint* Égrève*")
    }

    @Test
    fun `query syntax in the text is read as a word separator`() {
        assertThat("Thônes (74280)".addWildcard()).isEqualTo("Thônes* 74280*")
        assertThat("Saint-Égrève".addWildcard()).isEqualTo("Saint* Égrève*")
        assertThat("Mont@Saint|Martin".addWildcard()).isEqualTo("Mont* Saint* Martin*")
        assertThat("""a:b;c,d.e/f\g"h'i[j]k{l}m~n!o*p%q^r&s=t+u<v>w?x#y${'$'}z""".addWildcard())
            .isEqualTo("a b c d e f g h i j k l m n o p q r s t u v w x y z")
    }

    @Test
    fun `a single letter is searched as a word, below the shortest prefix RediSearch expands`() {
        assertThat("L'Isle-d'Abeau".addWildcard()).isEqualTo("L Isle* d Abeau*")
    }

    @Test
    fun `an accent typed as a combining mark stays inside its word`() {
        assertThat("Thônes".addWildcard()).isEqualTo("Thônes*")
    }

    @Test
    fun `an underscore stays inside its word, as RediSearch indexes it`() {
        assertThat("Mont_du_Lac".addWildcard()).isEqualTo("Mont_du_Lac*")
    }

    @Test
    fun `no text is no query`() {
        assertThat(null.addWildcard()).isEqualTo("")
        assertThat("  - ( ) ".addWildcard()).isEqualTo("")
    }

    @Test
    fun `a blank query matches every document`() {
        assertThat(null.orWildcard()).isEqualTo("*")
        assertThat("  ".orWildcard()).isEqualTo("*")
        assertThat("name*".orWildcard()).isEqualTo("name*")
    }

    @Test
    fun `a tag clause refuses an empty value, no value or an attribute name that is not one`() {
        assertThatThrownBy { val _ = tagClause("status", "") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { val _ = tagClause("status", emptyList()) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { val _ = tagClause("status", listOf("Open", "")) }.isInstanceOf(IllegalArgumentException::class.java)
        listOf("", "a b", "a}|@b", "1st", "@status").forEach { field ->
            assertThatThrownBy { val _ = tagClause(field, "x") }
                .describedAs("«$field»")
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
