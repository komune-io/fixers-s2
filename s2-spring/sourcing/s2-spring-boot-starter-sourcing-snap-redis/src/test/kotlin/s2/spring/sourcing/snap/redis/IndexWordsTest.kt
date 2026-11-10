package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A name read the way the index reads a term, for names the index does not hold. */
class IndexWordsTest {

    @Test
    fun `every word of the term opens a word of the name, accents and case aside`() {
        assertThat(IndexWords.holds(name = "Monnet-Sève Sougy", term = "monnet seve")).isTrue()
        assertThat(IndexWords.holds(name = "Monnet-Sève Sougy", term = "SOUG")).isTrue()
        assertThat(IndexWords.holds(name = "Monnet-Sève Sougy", term = "monnet lac")).isFalse()
        assertThat(IndexWords.holds(name = "Monnet-Sève Sougy", term = "onnet")).isFalse()
    }

    @Test
    fun `a word under the minimum prefix opens only the same word`() {
        assertThat(IndexWords.holds(name = "A Lac", term = "a")).isTrue()
        assertThat(IndexWords.holds(name = "Atelier Lac", term = "a")).isFalse()
        assertThat(IndexWords.holds(name = "Atelier Lac", term = "at")).isTrue()
    }

    @Test
    fun `a term without a word holds nothing`() {
        assertThat(IndexWords.holds(name = "Atelier Lac", term = " - ")).isFalse()
    }
}
