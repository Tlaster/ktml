package moe.tlaster.ktml.parser

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import moe.tlaster.ktml.Ktml
import moe.tlaster.ktml.parser.token.*

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

    @Test
    fun testYoutube() = runTest {
        val result = HttpClient().get("https://www.youtube.com/watch?v=wWltASCJO-U")
        val html = result.bodyAsText()
        val tokens = Ktml.tokenize(html)
        assertTrue {
            tokens.isNotEmpty()
        }
    }

    @Test
    fun testGoogle() = runTest {
        val result = HttpClient().get("https://www.google.com/")
        val html = result.bodyAsText()
        val tokens = Ktml.tokenize(html)
        assertTrue {
            tokens.isNotEmpty()
        }
    }

    @Test
    fun testGoogleSearch() = runTest {
        val result = HttpClient().get("https://www.google.com/search?q=kotlin")
        val html = result.bodyAsText()
        val tokens = Ktml.tokenize(html)
        assertTrue {
            tokens.isNotEmpty()
        }
    }

    @Test
    fun testEHentai() = runTest {
        val result = HttpClient().get("https://e-hentai.org/")
        val html = result.bodyAsText()
        val tokens = Ktml.tokenize(html)
        assertTrue {
            tokens.isNotEmpty()
        }
    }

    @Test
    fun testBilibili() = runTest {
        val result = HttpClient().get("https://www.bilibili.com/")
        val html = result.bodyAsText()
        val tokens = Ktml.tokenize(html)
        assertTrue {
            tokens.isNotEmpty()
        }
    }
}