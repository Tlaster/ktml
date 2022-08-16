package moe.tlaster.ktml.parser

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import moe.tlaster.ktml.Ktml

class ParserTest {
    @Test
    fun testGoogle() = runTest {

        val result = HttpClient().get("https://www.google.com/")
        val html = result.bodyAsText()
        val token = Ktml.parse(html)
        assertTrue {
            token.name == "html"
        }
    }
}