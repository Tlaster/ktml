package moe.tlaster.ktml

import moe.tlaster.ktml.dom.Element
import moe.tlaster.ktml.parser.SimpleParser
import moe.tlaster.ktml.parser.StringReader
import moe.tlaster.ktml.parser.Tokenizer
import moe.tlaster.ktml.parser.token.Token

object Ktml {
    internal fun tokenize(text: String): List<Token> {
        val reader = StringReader(text)
        val tokenizer = Tokenizer()
        tokenizer.parse(reader)
        return tokenizer.tokens
    }

    fun parse(text: String): Element {
        val tokens = tokenize(text)
        return SimpleParser.parse(tokens)
    }
}
