package moe.tlaster.ktml

import moe.tlaster.ktml.parser.StringReader
import moe.tlaster.ktml.parser.Tokenizer
import moe.tlaster.ktml.parser.eof
import moe.tlaster.ktml.parser.token.Token

object Ktml {
    internal fun tokenize(text: String): List<Token> {
        val reader = StringReader(text + eof)
        val tokenizer = Tokenizer()
        tokenizer.parse(reader)
        return tokenizer.tokens
    }
}