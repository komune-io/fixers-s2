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
        assertThat(tagClause(field = "status", value = "Deleted")).isEqualTo("@status:{Deleted}")
    }

    @Test
    fun `a value carrying query syntax stays inside its braces`() {
        assertThat(tagClause(field = "code", value = "38} | @status:{Deleted"))
            .isEqualTo("""@code:{38\}\ \|\ \@status\:\{Deleted}""")
    }

    @Test
    fun `every punctuation mark of a value is escaped`() {
        assertThat(tagClause(field = "f", value = "a-b.c(d)*e,f'g\"h\\i"))
            .isEqualTo("""@f:{a\-b\.c\(d\)\*e\,f\'g\"h\\i}""")
    }

    @Test
    fun `letters of any alphabet, digits and underscores are not escaped`() {
        assertThat(tagClause(field = "city", value = "Saint_Égrève2")).isEqualTo("@city:{Saint_Égrève2}")
    }

    @Test
    fun `a union keeps its separators and escapes each member`() {
        assertThat(tagClause(field = "id", values = listOf("a-1", "b|c"))).isEqualTo("""@id:{a\-1|b\|c}""")
    }

    @Test
    fun `each word becomes a prefix, its accents and capitals folded as the index folds them`() {
        assertThat("Saint Égrève".addWildcard()).isEqualTo("saint* egreve*")
        assertThat("ŒUVRE".addWildcard()).isEqualTo("œuvre*")
    }

    @Test
    fun `query syntax in the text is read as a word separator`() {
        assertThat("Thônes (74280)".addWildcard()).isEqualTo("thones* 74280*")
        assertThat("Saint-Égrève".addWildcard()).isEqualTo("saint* egreve*")
        assertThat("Mont@Saint|Martin".addWildcard()).isEqualTo("mont* saint* martin*")
        assertThat("""a:b;c,d.e/f\g"h'i[j]k{l}m~n!o*p%q^r&s=t+u<v>w?x#y${'$'}z""".addWildcard())
            .isEqualTo("a b c d e f g h i j k l m n o p q r s t u v w x y z")
    }

    @Test
    fun `a single letter is searched as a word, below the shortest prefix RediSearch expands`() {
        assertThat("L'Isle-d'Abeau".addWildcard()).isEqualTo("l isle* d abeau*")
    }

    @Test
    fun `an accent typed as a combining mark is folded inside its word`() {
        assertThat("Tho\u0302nes".addWildcard()).isEqualTo("thones*")
        assertThat("e\u0301".addWildcard()).isEqualTo("e")
    }

    @Test
    fun `an underscore stays inside its word, as RediSearch indexes it`() {
        assertThat("Mont_du_Lac".addWildcard()).isEqualTo("mont_du_lac*")
    }

    @Test
    fun `no text is no query`() {
        assertThat(null.addWildcard()).isEqualTo("")
        assertThat("  - ( ) ".addWildcard()).isEqualTo("")
    }

    @Test
    fun `a query's words are folded, its tags, ranges, attribute names and parameters kept as written`() {
        assertThat(foldQuery("""Érable @Name:(ÉGRÈ* | "Saint Étienne") -Œuvre"""))
            .isEqualTo("""erable @Name:(egre* | "saint etienne") -œuvre""")
        assertThat(foldQuery("""@status:{Égrève|A\}É} Été @rank:[-inf (10] @geo:[2.3 48.8 5 KM] ${'$'}Param"""))
            .isEqualTo("""@status:{Égrève|A\}É} ete @rank:[-inf (10] @geo:[2.3 48.8 5 KM] ${'$'}Param""")
        assertThat(foldQuery("""a\{É \@Été""")).isEqualTo("""a\{e \@ete""")
        assertThat(foldQuery("@Name|Title:Été")).isEqualTo("@Name|Title:ete")
        assertThat(foldQuery("*")).isEqualTo("*")
        assertThat(foldQuery("@name:{unclosed É")).isEqualTo("@name:{unclosed É")
    }

    @Test
    fun `a text is folded as the index holds it, and IndexWords reads it alike`() {
        assertThat(foldText("Saint-Égrève ŒUVRE Ørsted İstanbul e\u0301"))
            .isEqualTo("saint-egreve œuvre ørsted istanbul e")
        assertThat(foldText("한국 Ä")).isEqualTo("한국 a")
        assertThat(IndexWords.holds(name = "Œuvre Ørsted", term = "ŒUV ØRST")).isTrue()
    }

    @Test
    fun `a blank query matches every document`() {
        assertThat(null.orWildcard()).isEqualTo("*")
        assertThat("  ".orWildcard()).isEqualTo("*")
        assertThat("name*".orWildcard()).isEqualTo("name*")
    }

    @Test
    fun `a tag clause refuses an empty value, no value or an attribute name that is not one`() {
        assertThatThrownBy { val _ = tagClause(field = "status", value = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { val _ = tagClause(field = "status", values = emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { val _ = tagClause(field = "status", values = listOf("Open", "")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        listOf("", "a b", "a}|@b", "1st", "@status").forEach { field ->
            assertThatThrownBy { val _ = tagClause(field = field, value = "x") }
                .describedAs("«$field»")
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `an application may start prefixes later, never earlier than RediSearch expands them`() {
        assertThat("ab abc Saint".addWildcard(minPrefix = 3)).isEqualTo("ab abc* saint*")
        assertThatThrownBy { val _ = "ab".addWildcard(minPrefix = 1) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { val _ = IndexSettings(phonetic = null, wildcardMinPrefix = 1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
