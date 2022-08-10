package moe.tlaster.ktml.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import moe.tlaster.ktml.Ktml
import moe.tlaster.ktml.parser.token.*

class TokenizerTest {

    @Test
    fun testSimpleHtml() {
        val html = "<html><body><div>Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            TagOpen,
            Tag('h'),
            Tag('t'),
            Tag('m'),
            Tag('l'),
            TagClose,
            TagOpen,
            Tag('b'),
            Tag('o'),
            Tag('d'),
            Tag('y'),
            TagClose,
            TagOpen,
            Tag('d'),
            Tag('i'),
            Tag('v'),
            TagClose,
            Character('H'),
            Character('e'),
            Character('l'),
            Character('l'),
            Character('o'),
            EndTagOpen,
            Tag('d'),
            Tag('i'),
            Tag('v'),
            TagClose,
            EndTagOpen,
            Tag('b'),
            Tag('o'),
            Tag('d'),
            Tag('y'),
            TagClose,
            EndTagOpen,
            Tag('h'),
            Tag('t'),
            Tag('m'),
            Tag('l'),
            TagClose,
            EOF,
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testHtmlWithAttributes() {
        val html = "<html><body><div id=\"hello\" class=\"world\">Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            TagOpen,
            Tag('h'),
            Tag('t'),
            Tag('m'),
            Tag('l'),
            TagClose,
            TagOpen,
            Tag('b'),
            Tag('o'),
            Tag('d'),
            Tag('y'),
            TagClose,
            TagOpen,
            Tag('d'),
            Tag('i'),
            Tag('v'),
            AttributeOpen,
            Attribute('i'),
            Attribute('d'),
            AttributeValue('h'),
            AttributeValue('e'),
            AttributeValue('l'),
            AttributeValue('l'),
            AttributeValue('o'),
            AttributeOpen,
            Attribute('c'),
            Attribute('l'),
            Attribute('a'),
            Attribute('s'),
            Attribute('s'),
            AttributeValue('w'),
            AttributeValue('o'),
            AttributeValue('r'),
            AttributeValue('l'),
            AttributeValue('d'),
            TagClose,
            Character('H'),
            Character('e'),
            Character('l'),
            Character('l'),
            Character('o'),
            EndTagOpen,
            Tag('d'),
            Tag('i'),
            Tag('v'),
            TagClose,
            EndTagOpen,
            Tag('b'),
            Tag('o'),
            Tag('d'),
            Tag('y'),
            TagClose,
            EndTagOpen,
            Tag('h'),
            Tag('t'),
            Tag('m'),
            Tag('l'),
            TagClose,
            EOF,
        )
        assertContentEquals(expectTokens, tokens)
    }
}