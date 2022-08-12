package moe.tlaster.ktml.parser

import moe.tlaster.ktml.parser.error.Error
import moe.tlaster.ktml.parser.states.DataState
import moe.tlaster.ktml.parser.states.State
import moe.tlaster.ktml.parser.states.inAttribute
import moe.tlaster.ktml.parser.token.*
import moe.tlaster.ktml.parser.token.AttributeValueChar
import moe.tlaster.ktml.parser.token.Character
import moe.tlaster.ktml.parser.token.CharacterReferenceClose
import moe.tlaster.ktml.parser.token.CharacterReferenceCodeAdd
import moe.tlaster.ktml.parser.token.CharacterReferenceCodeMultiples
import moe.tlaster.ktml.parser.token.CharacterReferenceOpen
import moe.tlaster.ktml.parser.token.TokenCharacter

internal class Tokenizer {
    lateinit var returnState: State
    var currentState: State = DataState
    var characterReferenceCode: Int = 0
    val errors = arrayListOf<Error>()
    private var buffer = StringBuilder()
    val tokens = arrayListOf<Token>()
    private val tokenBuilders = arrayListOf<TokenBuilder>()
    fun parse(reader: Reader) {
        while (reader.hasNext()) {
            currentState.read(this, reader)
        }
    }

    fun emit(tokenCharacter: TokenCharacter) {
        when (tokenCharacter) {
            CharacterReferenceClose -> Unit
            is CharacterReferenceCodeAdd -> characterReferenceCode += tokenCharacter.value
            is CharacterReferenceCodeMultiples -> characterReferenceCode *= tokenCharacter.by
            CharacterReferenceOpen -> characterReferenceCode = 0
            TagOpen -> {
                tokenBuilders.add(TagBuilder())
            }
            is TagChar -> {
                when (val last = tokenBuilders.lastOrNull()) {
                    is TagBuilder -> last.name.append(tokenCharacter.char)
                    is EndTagBuilder -> last.name.append(tokenCharacter.char)
                    else -> throw Exception("Unexpected token $last")
                }
            }
            TagClose -> {
                val last = tokenBuilders.lastOrNull()
                require(last is TagBuilder || last is EndTagBuilder || last is AttributeBuilder) { "last token is not a tag, is $last, tokens: $tokenBuilders" }
            }
            EndTagOpen -> {
                tokenBuilders.add(EndTagBuilder())
            }
            CommentOpen -> {
                tokenBuilders.add(CommentBuilder())
            }
            is CommentChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is CommentBuilder) { "last token is not a comment, is $last, tokens: $tokenBuilders" }
                last.content.append(tokenCharacter.char)
            }
            is CommentClose -> {
                val last = tokenBuilders.lastOrNull()
                require(last is CommentBuilder) { "last token is not a comment, is $last, tokens: $tokenBuilders" }
            }
            DocTypeOpen -> {
                tokenBuilders.add(DocTypeBuilder())
            }
            is DocTypeChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is DocTypeBuilder) { "last token is not a doctype, is $last, tokens: $tokenBuilders" }
                last.name.append(tokenCharacter.char)
            }
            is DocTypeClose -> {
                val last = tokenBuilders.lastOrNull()
                require(last is DocTypeBuilder) { "last token is not a doctype, is $last, tokens: $tokenBuilders" }
            }
            is DocTypePublicIdentifierChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is DocTypeBuilder) { "last token is not a doctype, is $last, tokens: $tokenBuilders" }
                last.publicIdentifier.append(tokenCharacter.char)
            }
            is DocTypeSystemIdentifierChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is DocTypeBuilder) { "last token is not a doctype, is $last, tokens: $tokenBuilders" }
                last.systemIdentifier.append(tokenCharacter.char)
            }
            AttributeOpen -> {
                tokenBuilders.add(AttributeBuilder())
            }
            is AttributeChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is AttributeBuilder) { "last token is not an attribute, is $last, tokens: $tokenBuilders" }
                last.name.append(tokenCharacter.char)
            }
            is AttributeValueChar -> {
                val last = tokenBuilders.lastOrNull()
                require(last is AttributeBuilder) { "last token is not an attribute, is $last, tokens: $tokenBuilders" }
                last.value.append(tokenCharacter.char)
            }
            ForceQuirks -> {
                val last = tokenBuilders.lastOrNull()
                require(last is DocTypeBuilder) { "last token is not a tag, is $last, tokens: $tokenBuilders" }
                last.forceQuirks = true
            }
            is Character -> {
                val last = tokenBuilders.lastOrNull()
                if (last is TextBuilder) {
                    last.content.append(tokenCharacter.c)
                } else {
                    tokenBuilders.add(TextBuilder(StringBuilder().apply { append(tokenCharacter.c) }))
                }
            }
            EOF -> buildTokens()
            TagSelfClose -> Unit
        }
    }

    private fun buildTokens() {
        tokenBuilders.forEach {
            tokens.add(it.build())
        }
    }

    fun switch(state: State) {
        currentState = state
    }

    fun error(error: Error) {
        errors += error
    }

    fun createTempBuffer() {
        buffer.clear()
    }

    fun appendToTempBuffer(char: Char) {
        buffer.append(char)
    }

    fun appendToTempBuffer(value: String) {
        buffer.append(value)
    }

    fun getTempBuffer(): List<Char> {
        return buffer.toString().toList()
    }

    fun getTempBufferString(): String {
        return buffer.toString()
    }

    fun flushTempBuffer() {
        if (returnState.inAttribute) {
            getTempBuffer().forEach {
                emit(AttributeValueChar(it))
            }
        } else {
            getTempBuffer().forEach {
                emit(Character(it))
            }
        }
    }

    fun isAppropriateEndTagToken(): Boolean {
        val last = tokenBuilders.last()
        val lastOpenTag = tokenBuilders.last { it is TagBuilder } as TagBuilder
        return last is EndTagBuilder && last.name.toString() == lastOpenTag.name.toString()
    }

    // Workaround for Google && YouTube
    fun isInScript(): Boolean {
        val last = tokenBuilders.lastOrNull() ?: return false
        if (last !is TextBuilder) {
            return false
        }
        val lastOpenScriptTag = tokenBuilders.lastOrNull { it is TagBuilder && it.name.toString() == "script" } ?: return false
        val lastOpenScriptTagIndex = tokenBuilders.indexOf(lastOpenScriptTag)
        if (lastOpenScriptTagIndex == tokenBuilders.lastIndex - 1) {
            return true
        }
        for (i in lastOpenScriptTagIndex until tokenBuilders.size) {
            val token = tokenBuilders[i]
            if (token !is AttributeBuilder && token !is TagBuilder && token !is TextBuilder) {
                return false
            }
        }
        return true
    }
}