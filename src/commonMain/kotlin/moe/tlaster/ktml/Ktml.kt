package moe.tlaster.ktml

import moe.tlaster.ktml.dom.Node
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

    internal fun parse(text: String): Node {
        val tokens = tokenize(text)
        return SimpleParser.parse(tokens)
    }
}
