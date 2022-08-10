package moe.tlaster.ktml.parser

import moe.tlaster.ktml.parser.error.Error
import moe.tlaster.ktml.parser.states.DataState
import moe.tlaster.ktml.parser.states.State
import moe.tlaster.ktml.parser.states.inAttribute
import moe.tlaster.ktml.parser.token.*
import moe.tlaster.ktml.parser.token.AttributeValue
import moe.tlaster.ktml.parser.token.Character
import moe.tlaster.ktml.parser.token.CharacterReferenceClose
import moe.tlaster.ktml.parser.token.CharacterReferenceCodeAdd
import moe.tlaster.ktml.parser.token.CharacterReferenceCodeMultiples
import moe.tlaster.ktml.parser.token.CharacterReferenceOpen
import moe.tlaster.ktml.parser.token.Token

internal class Tokenizer {
    lateinit var returnState: State
    var currentState: State = DataState
    var characterReferenceCode: Int = 0
    val errors = arrayListOf<Error>()
    var buffer = StringBuilder()
    val tokens = arrayListOf<Token>()
    fun parse(reader: Reader) {
        while (reader.hasNext()) {
            currentState.read(this, reader)
        }
    }

    fun emit(token: Token) {
        when (token) {
            CharacterReferenceClose -> Unit
            is CharacterReferenceCodeAdd -> characterReferenceCode += token.value
            is CharacterReferenceCodeMultiples -> characterReferenceCode *= token.by
            CharacterReferenceOpen -> characterReferenceCode = 0
            else -> tokens.add(token)
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
        buffer.append(' ')
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
                emit(AttributeValue(it))
            }
        } else {
            getTempBuffer().forEach {
                emit(Character(it))
            }
        }
    }

    fun isAppropriateEndTagToken(): Boolean {
        return true
    }
}