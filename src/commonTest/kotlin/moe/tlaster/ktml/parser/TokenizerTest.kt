package moe.tlaster.ktml.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import moe.tlaster.ktml.Ktml
import moe.tlaster.ktml.parser.token.Attribute
import moe.tlaster.ktml.parser.token.EndTag
import moe.tlaster.ktml.parser.token.Tag
import moe.tlaster.ktml.parser.token.Text

class TokenizerTest {

    @Test
    fun testSimpleHtml() {
        val html = "<html><body><div>Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("html"),
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body"),
            EndTag("html")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testHtmlWithAttributes() {
        val html = "<html><body><div id=\"hello\" class=\"world\">Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("html"),
            Tag("body"),
            Tag("div"),
            Attribute("id", "hello"),
            Attribute("class", "world"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body"),
            EndTag("html")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testWithoutHtmlTag() {
        val html = "<body><div>Hello</div></body>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testTextFirst() {
        val html = "Hello<body><div>Hello</div></body>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Text("Hello"),
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testScript() {
        val html = "<html><body><script>console.log('Hello')</script></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("html"),
            Tag("body"),
            Tag("script"),
            Text("console.log('Hello')"),
            EndTag("script"),
            EndTag("body"),
            EndTag("html")
        )
        assertContentEquals(expectTokens, tokens)
    }

}