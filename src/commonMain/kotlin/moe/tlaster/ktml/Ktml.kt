package moe.tlaster.ktml

import moe.tlaster.ktml.parser.StringReader
import moe.tlaster.ktml.parser.Tokenizer
import moe.tlaster.ktml.parser.token.Token
import moe.tlaster.ktml.parser.token.TokenCharacter

object Ktml {
    internal fun tokenize(text: String): List<Token> {
        val reader = StringReader(text)
        val tokenizer = Tokenizer()
        tokenizer.parse(reader)
        return tokenizer.tokens
    }
}