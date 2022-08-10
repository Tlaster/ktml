package moe.tlaster.ktml.parser.states

import moe.tlaster.ktml.parser.Reader
import moe.tlaster.ktml.parser.Tokenizer
import moe.tlaster.ktml.parser.eof
import moe.tlaster.ktml.parser.error.*
import moe.tlaster.ktml.parser.error.InvalidFirstCharacterOfTagName
import moe.tlaster.ktml.parser.error.MissingEndTagName
import moe.tlaster.ktml.parser.error.UnexpectedNullCharacterError
import moe.tlaster.ktml.parser.error.UnexpectedQuestionMarkInsteadOfTagName
import moe.tlaster.ktml.parser.token.*
import moe.tlaster.ktml.parser.token.Character
import moe.tlaster.ktml.parser.token.EOF

internal sealed interface State {
    fun read(tokenizer: Tokenizer, reader: Reader)
}

private val asciiUppercase = 'A'..'Z'
private val asciiLowercase = 'a'..'z'
private val asciiAlpha = asciiUppercase + asciiLowercase
private val asciiDigit = '0'..'9'
private val asciiAlphanumeric = asciiAlpha + asciiDigit
private val asciiUpperHexDigit = 'A'..'F'
private val asciiLowerHexDigit = 'a'..'f'
private val asciiHexDigit = asciiUpperHexDigit + asciiLowerHexDigit
private val c0control = 0x00..0x001F
private val control = c0control + (0x007F..0x009F)
private const val NULL = '\u0000'

internal object DataState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '<' -> tokenizer.switch(TagOpenState)
            '&' -> {
                tokenizer.returnState = DataState
                tokenizer.switch(CharacterReferenceState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character(current))
            }

            eof -> tokenizer.emit(EOF)
            else -> tokenizer.emit(Character(current))
        }
    }
}

internal object RcDataState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '&' -> {
                tokenizer.returnState = RcDataState
                tokenizer.switch(CharacterReferenceState)
            }

            '<' -> tokenizer.switch(RcDataLessThanSignState)
            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
            }

            eof -> tokenizer.emit(EOF)
            else -> tokenizer.emit(Character(current))
        }
    }
}

internal object RawTextState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '<' -> tokenizer.switch(RawTextLessThanSignState)
            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
            }

            eof -> tokenizer.emit(EOF)
            else -> tokenizer.emit(Character(current))
        }
    }
}

internal object ScriptDataState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '<' -> tokenizer.switch(ScriptDataLessThanSignState)
            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
            }

            eof -> tokenizer.emit(EOF)
            else -> tokenizer.emit(Character(current))
        }
    }
}

internal object PlainTextState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
            }

            eof -> tokenizer.emit(EOF)
            else -> tokenizer.emit(Character(current))
        }
    }
}

internal object TagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '!' -> tokenizer.switch(MarkupDeclarationOpenState)
            '/' -> tokenizer.switch(EndTagOpenState)
            in asciiAlpha -> {
                tokenizer.emit(TagOpen)
                tokenizer.switch(TagNameState)
                reader.pushback(current)
            }

            '?' -> {
                tokenizer.error(UnexpectedQuestionMarkInsteadOfTagName(reader.position))
                tokenizer.emit(CommentOpen)
                tokenizer.switch(BogusCommentState)
                reader.pushback(current)
            }

            eof -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(InvalidFirstCharacterOfTagName(reader.position))
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(DataState)
                reader.pushback(current)
            }
        }
    }
}

internal object EndTagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(EndTagOpen)
                tokenizer.switch(TagNameState)
                reader.pushback(current)
            }

            '>' -> {
                tokenizer.error(MissingEndTagName(reader.position))
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EndBeforeTagName(reader.position))
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u002F'))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(InvalidFirstCharacterOfTagName(reader.position))
                tokenizer.emit(CommentOpen)
                tokenizer.switch(BogusCommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object TagNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeAttributeNameState)
            }

            '/' -> {
                tokenizer.switch(SelfClosingStartTagState)
            }

            '>' -> {
                tokenizer.emit(TagClose)
                tokenizer.switch(DataState)
            }

            in asciiUppercase -> {
                tokenizer.emit(Tag(current.lowercaseChar()))
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Tag('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Tag(current))
            }
        }
    }
}

internal object RcDataLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '/' -> {
                tokenizer.createTempBuffer()
                tokenizer.switch(RcDataEndTagOpenState)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(RcDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object RcDataEndTagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(EndTagOpen)
                tokenizer.switch(RcDataEndTagNameState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u002F'))
                tokenizer.switch(RcDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object RcDataEndTagNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(BeforeAttributeNameState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '/' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(SelfClosingStartTagState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '>' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.emit(TagClose)
                    tokenizer.switch(DataState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            in asciiAlpha -> {
                tokenizer.emit(Tag(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                anythingElse(tokenizer, reader, current)
            }
        }
    }

    private fun anythingElse(tokenizer: Tokenizer, reader: Reader, current: Char) {
        tokenizer.emit(Character('\u003C'))
        tokenizer.emit(Character('\u002F'))
        tokenizer.getTempBuffer().forEach {
            tokenizer.emit(Character(it))
        }
        tokenizer.switch(RcDataState)
        reader.pushback(current)
    }
}

internal object RawTextLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '/' -> {
                tokenizer.createTempBuffer()
                tokenizer.switch(RawTextEndTagOpenState)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(RawTextState)
                reader.pushback(current)
            }
        }
    }
}

internal object RawTextEndTagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(EndTagOpen)
                tokenizer.switch(RawTextEndTagNameState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u002F'))
                tokenizer.switch(RawTextState)
                reader.pushback(current)
            }
        }
    }
}

internal object RawTextEndTagNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(BeforeAttributeNameState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '/' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(SelfClosingStartTagState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '>' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.emit(TagClose)
                    tokenizer.switch(DataState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            in asciiAlpha -> {
                tokenizer.emit(Tag(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                anythingElse(tokenizer, reader, current)
            }
        }
    }

    private fun anythingElse(tokenizer: Tokenizer, reader: Reader, current: Char) {
        tokenizer.emit(Character('\u003C'))
        tokenizer.emit(Character('\u002F'))
        tokenizer.getTempBuffer().forEach {
            tokenizer.emit(Character(it))
        }
        tokenizer.switch(RawTextState)
        reader.pushback(current)
    }
}

internal object ScriptDataLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '/' -> {
                tokenizer.createTempBuffer()
                tokenizer.switch(ScriptDataEndTagOpenState)
            }

            '!' -> {
                tokenizer.switch(ScriptDataEscapeStartState)
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u0021'))
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEndTagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(EndTagOpen)
                tokenizer.switch(ScriptDataEndTagNameState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u002F'))
                tokenizer.switch(ScriptDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEndTagNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(BeforeAttributeNameState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '/' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(SelfClosingStartTagState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '>' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.emit(TagClose)
                    tokenizer.switch(DataState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            in asciiAlpha -> {
                tokenizer.emit(Tag(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                anythingElse(tokenizer, reader, current)
            }
        }
    }

    private fun anythingElse(tokenizer: Tokenizer, reader: Reader, current: Char) {
        tokenizer.emit(Character('\u003C'))
        tokenizer.emit(Character('\u002F'))
        tokenizer.getTempBuffer().forEach {
            tokenizer.emit(Character(it))
        }
        tokenizer.switch(ScriptDataState)
        reader.pushback(current)
    }
}

internal object ScriptDataEscapeStartState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(ScriptDataEscapeStartDashState)
                tokenizer.emit(Character('\u002D'))
            }

            else -> {
                tokenizer.switch(ScriptDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEscapeStartDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(ScriptDataEscapedDashDashState)
                tokenizer.emit(Character('\u002D'))
            }

            else -> {
                tokenizer.switch(ScriptDataState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEscapedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
                tokenizer.switch(ScriptDataEscapedDashState)
            }

            '<' -> {
                tokenizer.switch(ScriptDataEscapedLessThanSignState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
                tokenizer.switch(ScriptDataEscapedState)
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
            }
        }
    }
}

internal object ScriptDataEscapedDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
                tokenizer.switch(ScriptDataEscapedDashDashState)
            }

            '<' -> {
                tokenizer.switch(ScriptDataEscapedLessThanSignState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
                tokenizer.switch(ScriptDataEscapedState)
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
                tokenizer.switch(ScriptDataEscapedState)
            }
        }
    }
}

internal object ScriptDataEscapedDashDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
            }

            '<' -> {
                tokenizer.switch(ScriptDataEscapedLessThanSignState)
            }

            '>' -> {
                tokenizer.emit(Character('\u003E'))
                tokenizer.switch(ScriptDataState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
                tokenizer.switch(ScriptDataEscapedState)
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
                tokenizer.switch(ScriptDataEscapedState)
            }
        }
    }
}

internal object ScriptDataEscapedLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '/' -> {
                tokenizer.createTempBuffer()
                tokenizer.switch(ScriptDataEscapedEndTagOpenState)
            }

            in asciiAlpha -> {
                tokenizer.createTempBuffer()
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataDoubleEscapeStartState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataEscapedState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEscapedEndTagOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(EndTagOpen)
                tokenizer.switch(ScriptDataEscapedEndTagNameState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.emit(Character('\u002F'))
                tokenizer.switch(ScriptDataEscapedState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataEscapedEndTagNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(BeforeAttributeNameState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '/' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.switch(SelfClosingStartTagState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            '>' -> {
                if (tokenizer.isAppropriateEndTagToken()) {
                    tokenizer.emit(TagClose)
                    tokenizer.switch(DataState)
                } else {
                    anythingElse(tokenizer, reader, current)
                }
            }

            in asciiAlpha -> {
                tokenizer.emit(Tag(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                anythingElse(tokenizer, reader, current)
            }
        }
    }

    private fun anythingElse(tokenizer: Tokenizer, reader: Reader, current: Char) {
        tokenizer.emit(Character('\u003C'))
        tokenizer.emit(Character('\u002F'))
        tokenizer.emit(Character(current))
        tokenizer.switch(ScriptDataEscapedState)
        reader.pushback(current)
    }
}

internal object ScriptDataDoubleEscapeStartState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020', '\u002F', '\u003E' -> {
                if (tokenizer.getTempBufferString() == "script") {
                    tokenizer.switch(ScriptDataDoubleEscapeEndState)
                } else {
                    tokenizer.switch(ScriptDataEscapedState)
                }
                tokenizer.emit(Character(current))
            }

            in asciiAlpha -> {
                tokenizer.emit(Character(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                tokenizer.switch(ScriptDataEscapedState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataDoubleEscapedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
                tokenizer.switch(ScriptDataDoubleEscapedDashState)
            }

            '<' -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataDoubleEscapedLessThanSignState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
            }
        }
    }
}

internal object ScriptDataDoubleEscapedDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
                tokenizer.switch(ScriptDataDoubleEscapedDashDashState)
            }

            '<' -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataDoubleEscapedLessThanSignState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
                tokenizer.switch(ScriptDataDoubleEscapedState)
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
                tokenizer.switch(ScriptDataDoubleEscapedState)
            }
        }
    }
}

internal object ScriptDataDoubleEscapedDashDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Character('\u002D'))
            }

            '<' -> {
                tokenizer.emit(Character('\u003C'))
                tokenizer.switch(ScriptDataDoubleEscapedLessThanSignState)
            }

            '>' -> {
                tokenizer.emit(Character('\u003E'))
                tokenizer.switch(ScriptDataState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Character('\uFFFD'))
                tokenizer.switch(ScriptDataDoubleEscapedState)
            }

            eof -> {
                tokenizer.error(EofInScriptHtmlCommentLikeText(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
                tokenizer.switch(ScriptDataDoubleEscapedState)
            }
        }
    }
}

internal object ScriptDataDoubleEscapedLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '/' -> {
                tokenizer.createTempBuffer()
                tokenizer.switch(ScriptDataDoubleEscapeEndState)
                tokenizer.emit(Character('\u002F'))
            }

            else -> {
                tokenizer.switch(ScriptDataDoubleEscapedState)
                reader.pushback(current)
            }
        }
    }
}

internal object ScriptDataDoubleEscapeEndState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020', '\u002F', '\u003E' -> {
                if (tokenizer.getTempBufferString() == "script") {
                    tokenizer.switch(ScriptDataEscapedState)
                } else {
                    tokenizer.switch(ScriptDataDoubleEscapedState)
                }
                tokenizer.emit(Character(current))
            }

            in asciiAlpha -> {
                tokenizer.emit(Character(current.lowercaseChar()))
                tokenizer.appendToTempBuffer(current)
            }

            else -> {
                tokenizer.switch(ScriptDataDoubleEscapedState)
                reader.pushback(current)
            }
        }
    }
}

internal object BeforeAttributeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '/', '>', eof -> {
                tokenizer.switch(AfterAttributeNameState)
                reader.pushback(current)
            }

            '=' -> {
                tokenizer.error(UnexpectedEqualsSignBeforeAttributeName(reader.position))
                tokenizer.emit(AttributeOpen)
                tokenizer.emit(Character(current))
                tokenizer.switch(AttributeNameState)
            }

            else -> {
                tokenizer.emit(AttributeOpen)
                tokenizer.switch(AttributeNameState)
                reader.pushback(current)
            }
        }
    }
}

internal object AttributeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020', '\u002F', '\u003E', eof -> {
                tokenizer.switch(AfterAttributeNameState)
                reader.pushback(current)
            }

            '=' -> {
                tokenizer.switch(BeforeAttributeValueState)
            }

            in asciiUppercase -> {
                tokenizer.emit(Attribute(current.lowercaseChar()))
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Attribute('\uFFFD'))
            }

            '\u0022', '\u0027', '\u003C' -> {
                tokenizer.error(UnexpectedCharacterInAttributeName(reader.position))
                tokenizer.emit(Character(current))
            }

            else -> {
                tokenizer.emit(Attribute(current))
            }

        }
    }
}

internal object AfterAttributeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '/' -> {
                tokenizer.switch(SelfClosingStartTagState)
            }

            '>' -> {
                tokenizer.switch(DataState)
                tokenizer.emit(TagClose)
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(AttributeOpen)
                tokenizer.switch(AttributeNameState)
                reader.pushback(current)
            }
        }
    }
}

internal object BeforeAttributeValueState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '"' -> {
                tokenizer.switch(AttributeValueDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.switch(AttributeValueSingleQuotedState)
            }

            '>' -> {
                tokenizer.error(MissingAttributeValue(reader.position))
                tokenizer.emit(TagClose)
                tokenizer.switch(DataState)
            }

            else -> {
                tokenizer.switch(AttributeValueUnquotedState)
                reader.pushback(current)
            }
        }
    }
}

internal object AttributeValueDoubleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '"' -> {
                tokenizer.switch(AfterAttributeValueQuotedState)
            }

            '&' -> {
                tokenizer.returnState = AttributeValueDoubleQuotedState
                tokenizer.switch(CharacterReferenceState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(AttributeValue('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(AttributeValue(current))
            }
        }
    }
}

internal object AttributeValueSingleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\'' -> {
                tokenizer.switch(AfterAttributeValueQuotedState)
            }

            '&' -> {
                tokenizer.returnState = AttributeValueSingleQuotedState
                tokenizer.switch(CharacterReferenceState)
            }

            '\u0000' -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(AttributeValue('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(AttributeValue(current))
            }
        }
    }
}

internal object AttributeValueUnquotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeAttributeNameState)
            }

            '&' -> {
                tokenizer.returnState = AttributeValueUnquotedState
                tokenizer.switch(CharacterReferenceState)
            }

            '>' -> {
                tokenizer.emit(TagClose)
                tokenizer.switch(DataState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(AttributeValue('\uFFFD'))
            }

            '"', '\'', '<', '=', '`' -> {
                tokenizer.error(UnexpectedCharacterInUnquotedAttributeValue(reader.position))
                tokenizer.emit(AttributeValue(current))
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(AttributeValue(current))
            }
        }
    }
}

internal object AfterAttributeValueQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeAttributeNameState)
            }

            '/' -> {
                tokenizer.switch(SelfClosingStartTagState)
            }

            '>' -> {
                tokenizer.switch(DataState)
                tokenizer.emit(TagClose)
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingWhitespaceBetweenAttributes(reader.position))
                tokenizer.switch(BeforeAttributeNameState)
                reader.pushback(current)
            }
        }
    }
}

internal object SelfClosingStartTagState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '>' -> {
                tokenizer.emit(TagSelfClose)
                tokenizer.emit(TagClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInTag(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(UnexpectedSolidusInTag(reader.position))
                tokenizer.switch(BeforeAttributeNameState)
                reader.pushback(current)
            }
        }
    }
}

internal object BogusCommentState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '>' -> {
                tokenizer.emit(CommentClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Comment('\uFFFD'))
            }

            else -> {
                tokenizer.emit(Comment(current))
            }
        }
    }
}

//https://html.spec.whatwg.org/#markup-declaration-open-state
internal object MarkupDeclarationOpenState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when {
            reader.isFollowedBy("--") -> {
                tokenizer.switch(CommentStartState)
                tokenizer.emit(CommentOpen)
                reader.consume("--".length)
            }

            reader.isFollowedBy("DOCTYPE", ignoreCase = true) -> {
                tokenizer.switch(DoctypeState)
                reader.consume("DOCTYPE".length)
            }

            reader.isFollowedBy("[CDATA[") -> {
                //TODO: Check if there is an adjusted current node and it is not an element in the HTML namespace
                tokenizer.switch(CdataSectionState)
                reader.consume("[CDATA[".length)
            }

            else -> {
                tokenizer.error(IncorrectlyOpenedComment(reader.position))
                tokenizer.switch(BogusCommentState)
            }
        }
    }
}

internal object CommentStartState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentStartDashState)
            }

            '>' -> {
                tokenizer.error(AbruptClosingOfEmptyComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.switch(DataState)
            }

            else -> {
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentStartDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentEndState)
            }

            '>' -> {
                tokenizer.error(AbruptClosingOfEmptyComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Comment('-'))
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentEndDashState)
            }

            '<' -> {
                tokenizer.emit(Comment(current))
                tokenizer.switch(CommentLessThanSignState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(Comment('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Comment(current))
            }
        }
    }
}

internal object CommentLessThanSignState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '!' -> {
                tokenizer.emit(Comment(current))
                tokenizer.switch(CommentLessThanSignBangState)
            }

            '-' -> {
                tokenizer.emit(Comment(current))
            }

            else -> {
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentLessThanSignBangState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentLessThanSignBangDashState)
            }

            else -> {
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentLessThanSignBangDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentLessThanSignBangDashDashState)
            }

            else -> {
                tokenizer.switch(CommentEndDashState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentLessThanSignBangDashDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '>', eof -> {
                tokenizer.switch(CommentEndState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.error(NestedComment(reader.position))
                tokenizer.switch(CommentEndState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentEndDashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.switch(CommentEndState)
            }

            eof -> {
                tokenizer.error(EofInComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Comment('\u002D'))
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentEndState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '>' -> {
                tokenizer.emit(CommentClose)
                tokenizer.switch(DataState)
            }

            '!' -> {
                tokenizer.switch(CommentEndBangState)
            }

            '-' -> {
                tokenizer.emit(Comment('\u002D'))
            }

            eof -> {
                tokenizer.error(EofInComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Comment('\u002D'))
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object CommentEndBangState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '-' -> {
                tokenizer.emit(Comment('-'))
                tokenizer.emit(Comment('-'))
                tokenizer.emit(Comment('!'))
                tokenizer.switch(CommentEndDashState)
            }

            '>' -> {
                tokenizer.error(IncorrectlyClosedComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInComment(reader.position))
                tokenizer.emit(CommentClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Comment('-'))
                tokenizer.emit(Comment('!'))
                tokenizer.switch(CommentState)
                reader.pushback(current)
            }
        }
    }
}

internal object DoctypeState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeDoctypeNameState)
            }

            '>' -> {
                tokenizer.switch(BeforeDoctypeNameState)
                reader.pushback(current)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingWhitespaceBeforeDoctypeName(reader.position))
                tokenizer.switch(BeforeDoctypeNameState)
                reader.pushback(current)
            }
        }
    }
}

internal object BeforeDoctypeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            in asciiUppercase -> {
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(DocType(current.lowercaseChar()))
                tokenizer.switch(DoctypeNameState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(DocType('\uFFFD'))
                tokenizer.switch(DoctypeNameState)
            }

            '>' -> {
                tokenizer.error(MissingDoctypeName(reader.position))
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocTypeOpen)
                tokenizer.emit(DocType(current))
                tokenizer.switch(DoctypeNameState)
            }
        }
    }
}

internal object DoctypeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(AfterDoctypeNameState)
            }

            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            in asciiUppercase -> {
                tokenizer.emit(DocType(current.lowercaseChar()))
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocType('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocType(current))
            }

        }
    }
}

internal object AfterDoctypeNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                if (reader.isFollowedBy("public", ignoreCase = true)) {
                    reader.consume("public".length)
                    tokenizer.switch(AfterDoctypePublicKeywordState)
                } else if (reader.isFollowedBy("system", ignoreCase = true)) {
                    reader.consume("system".length)
                    tokenizer.switch(AfterDoctypeSystemKeywordState)
                } else {
                    tokenizer.error(InvalidCharacterSequenceAfterDoctypeName(reader.position))
                    tokenizer.emit(ForceQuirks)
                    tokenizer.switch(BogusDoctypeState)
                    reader.pushback(current)
                }
            }
        }
    }
}

internal object AfterDoctypePublicKeywordState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeDoctypePublicIdentifierState)
            }

            '"' -> {
                tokenizer.error(MissingWhitespaceAfterDoctypePublicKeyword(reader.position))
                tokenizer.emit(DocTypePublicIdentifier(' '))
                tokenizer.switch(DoctypePublicIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.error(MissingWhitespaceAfterDoctypePublicKeyword(reader.position))
                tokenizer.emit(DocTypePublicIdentifier(' '))
                tokenizer.switch(DoctypePublicIdentifierSingleQuotedState)
            }

            '>' -> {
                tokenizer.error(MissingDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object BeforeDoctypePublicIdentifierState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '"' -> {
                tokenizer.emit(DocTypePublicIdentifier(' '))
                tokenizer.switch(DoctypePublicIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.emit(DocTypePublicIdentifier(' '))
                tokenizer.switch(DoctypePublicIdentifierSingleQuotedState)
            }

            '>' -> {
                tokenizer.error(MissingDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object DoctypePublicIdentifierDoubleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '"' -> {
                tokenizer.switch(AfterDoctypePublicIdentifierState)
            }

            '>' -> {
                tokenizer.error(AbruptDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocTypePublicIdentifier('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocTypePublicIdentifier(current))
            }
        }
    }
}

internal object DoctypePublicIdentifierSingleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\'' -> {
                tokenizer.switch(AfterDoctypePublicIdentifierState)
            }

            '>' -> {
                tokenizer.error(AbruptDoctypePublicIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocTypePublicIdentifier('\uFFFD'))
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocTypePublicIdentifier(current))
            }
        }
    }
}

internal object AfterDoctypePublicIdentifierState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BetweenDoctypePublicAndSystemIdentifiersState)
            }

            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            '"' -> {
                tokenizer.error(MissingWhitespaceBetweenDoctypePublicAndSystemIdentifiers(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.error(MissingWhitespaceBetweenDoctypePublicAndSystemIdentifiers(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierSingleQuotedState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object BetweenDoctypePublicAndSystemIdentifiersState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            '"' -> {
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierSingleQuotedState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object AfterDoctypeSystemKeywordState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                tokenizer.switch(BeforeDoctypeSystemIdentifierState)
            }

            '"' -> {
                tokenizer.error(MissingWhitespaceAfterDoctypeSystemKeyword(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.error(MissingWhitespaceAfterDoctypeSystemKeyword(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierSingleQuotedState)
            }

            '>' -> {
                tokenizer.error(MissingDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object BeforeDoctypeSystemIdentifierState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '"' -> {
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierDoubleQuotedState)
            }

            '\'' -> {
                tokenizer.emit(DocTypeSystemIdentifier(' '))
                tokenizer.switch(DoctypeSystemIdentifierSingleQuotedState)
            }

            '>' -> {
                tokenizer.error(MissingDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(MissingQuoteBeforeDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object DoctypeSystemIdentifierDoubleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '"' -> {
                tokenizer.switch(AfterDoctypeSystemIdentifierState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier('\uFFFD'))
            }

            '>' -> {
                tokenizer.error(AbruptDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocTypeSystemIdentifier(current))
            }
        }
    }
}

internal object DoctypeSystemIdentifierSingleQuotedState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\'' -> {
                tokenizer.switch(AfterDoctypeSystemIdentifierState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
                tokenizer.emit(DocTypeSystemIdentifier('\uFFFD'))
            }

            '>' -> {
                tokenizer.error(AbruptDoctypeSystemIdentifier(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(DocTypeSystemIdentifier(current))
            }
        }
    }
}

internal object AfterDoctypeSystemIdentifierState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '\u0009', '\u000A', '\u000C', '\u0020' -> {
                // Ignore the character.
            }

            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            eof -> {
                tokenizer.error(EofInDoctype(reader.position))
                tokenizer.emit(ForceQuirks)
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.error(UnexpectedCharacterAfterDoctypeSystemIdentifier(reader.position))
                tokenizer.switch(BogusDoctypeState)
                reader.pushback(current)
            }
        }
    }
}

internal object BogusDoctypeState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            '>' -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.switch(DataState)
            }

            NULL -> {
                tokenizer.error(UnexpectedNullCharacterError(reader.position))
            }

            eof -> {
                tokenizer.emit(DocTypeClose)
                tokenizer.emit(EOF)
            }

            else -> {
                // Ignore the character.
            }
        }
    }
}

internal object CdataSectionState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            ']' -> {
                tokenizer.switch(CdataSectionBracketState)
            }

            eof -> {
                tokenizer.error(EofInCdata(reader.position))
                tokenizer.emit(EOF)
            }

            else -> {
                tokenizer.emit(Character(current))
            }
        }
    }
}

internal object CdataSectionBracketState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            ']' -> {
                tokenizer.switch(CdataSectionEndState)
            }

            else -> {
                tokenizer.emit(Character(']'))
                tokenizer.switch(CdataSectionState)
                reader.pushback(current)
            }
        }
    }
}

internal object CdataSectionEndState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            ']' -> {
                tokenizer.emit(Character(']'))
            }

            '>' -> {
                tokenizer.switch(DataState)
            }

            else -> {
                tokenizer.emit(Character(']'))
                tokenizer.switch(CdataSectionState)
                reader.pushback(current)
            }
        }
    }
}

internal object CharacterReferenceState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        tokenizer.createTempBuffer()
        tokenizer.appendToTempBuffer('&')
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                tokenizer.switch(NamedCharacterReferenceState)
                reader.pushback(current)
            }

            '#' -> {
                tokenizer.appendToTempBuffer('#')
                tokenizer.switch(NumericCharacterReferenceState)
            }

            else -> {
                tokenizer.flushTempBuffer()
                val returnState = tokenizer.returnState
                tokenizer.switch(returnState)
                reader.pushback(current)
            }
        }
    }
}

internal object NamedCharacterReferenceState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val consumedCharacters = StringBuilder()
        while (reader.hasNext()) {
            val current = reader.consume()
            consumedCharacters.append(current)
            val consumedCharactersString = consumedCharacters.toString()
            if (consumedCharactersString.startsWith("&") &&
                namedCharacters.containsKey(consumedCharactersString)
            ) {
                tokenizer.appendToTempBuffer(consumedCharactersString)
            }
            val returnState = tokenizer.returnState
            if (returnState.inAttribute && current != ';' && reader.hasNext() && (reader.next() in asciiAlphanumeric || reader.next() == '=')) {
                tokenizer.flushTempBuffer()
                tokenizer.switch(returnState)
                return
            }
            if (current != ';') {
                tokenizer.error(MissingSemicolonAfterCharacterReference(reader.position))
            }
            val buffer = tokenizer.getTempBufferString()
            tokenizer.createTempBuffer()
            namedCharacters[buffer]!!.forEach {
                tokenizer.appendToTempBuffer(it.toChar())
            }
            tokenizer.flushTempBuffer()
            tokenizer.switch(returnState)
            return
        }
        tokenizer.flushTempBuffer()
        tokenizer.switch(AmbiguousAmpersandState)
    }
}

internal object AmbiguousAmpersandState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                val returnState = tokenizer.returnState
                if (returnState.inAttribute) {
                    tokenizer.emit(AttributeValue(current))
                } else {
                    tokenizer.emit(Character(current))
                }
            }

            ';' -> {
                tokenizer.error(UnkonwnNamedCharacterReference(reader.position))
                tokenizer.switch(tokenizer.returnState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.switch(tokenizer.returnState)
                reader.pushback(current)
            }
        }
    }
}

internal object NumericCharacterReferenceState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        tokenizer.emit(CharacterReferenceOpen)
        when (val current = reader.consume()) {
            '\u0058', '\u0078' -> {
                tokenizer.appendToTempBuffer(current)
                tokenizer.switch(HexadecimalCharacterReferenceStartState)
            }

            else -> {
                tokenizer.switch(DecimalCharacterReferenceStartState)
                reader.pushback(current)
            }
        }
    }
}

internal object HexadecimalCharacterReferenceStartState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiHexDigit -> {
                tokenizer.switch(HexadecimalCharacterReferenceState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.error(AbsenceOfDigitInNumericCharacterReference(reader.position))
                tokenizer.flushTempBuffer()
                tokenizer.switch(tokenizer.returnState)
                reader.pushback(current)
            }
        }
    }
}

internal object DecimalCharacterReferenceStartState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiDigit -> {
                tokenizer.switch(DecimalCharacterReferenceState)
                reader.pushback(current)
            }

            else -> {
                tokenizer.error(AbsenceOfDigitInNumericCharacterReference(reader.position))
                tokenizer.flushTempBuffer()
                tokenizer.switch(tokenizer.returnState)
                reader.pushback(current)
            }
        }
    }
}

internal object HexadecimalCharacterReferenceState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiDigit -> {
                tokenizer.emit(CharacterReferenceCodeMultiples(16))
                tokenizer.emit(CharacterReferenceCodeAdd(current.code - 0x0030))
            }

            in asciiUpperHexDigit -> {
                tokenizer.emit(CharacterReferenceCodeMultiples(16))
                tokenizer.emit(CharacterReferenceCodeAdd(current.code - 0x0037))
            }

            in asciiLowerHexDigit -> {
                tokenizer.emit(CharacterReferenceCodeMultiples(16))
                tokenizer.emit(CharacterReferenceCodeAdd(current.code - 0x0057))
            }

            '\u003b' -> {
                tokenizer.switch(NumericCharacterReferenceEndState)
            }

            else -> {
                tokenizer.error(MissingSemicolonAfterCharacterReference(reader.position))
                tokenizer.switch(NumericCharacterReferenceEndState)
                reader.pushback(current)
            }
        }
    }
}

internal object DecimalCharacterReferenceState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiDigit -> {
                tokenizer.emit(CharacterReferenceCodeMultiples(10))
                tokenizer.emit(CharacterReferenceCodeAdd(current.code - 0x0030))
            }

            '\u003b' -> {
                tokenizer.switch(NumericCharacterReferenceEndState)
            }

            else -> {
                tokenizer.error(MissingSemicolonAfterCharacterReference(reader.position))
                tokenizer.switch(NumericCharacterReferenceEndState)
                reader.pushback(current)
            }
        }
    }
}

internal object NumericCharacterReferenceEndState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        tokenizer.emit(CharacterReferenceClose)
        when {
            tokenizer.characterReferenceCode == 0x00 -> {
                tokenizer.error(NullCharacterReference(reader.position))
                tokenizer.characterReferenceCode = 0xFFFD
            }

            tokenizer.characterReferenceCode > 0x10FFFF -> {
                tokenizer.error(CharacterReferenceOutOfRange(reader.position))
                tokenizer.characterReferenceCode = 0xFFFD
            }

            tokenizer.characterReferenceCode in 0xD800..0xDFFF -> {
                tokenizer.error(CharacterReferenceSurrogate(reader.position))
                tokenizer.characterReferenceCode = 0xFFFD
            }

            // non-character code points
            tokenizer.characterReferenceCode in 0xFDD0..0xFDEF -> {
                tokenizer.error(NonCharacterCharacterReference(reader.position))
            }

            tokenizer.characterReferenceCode == 0x0D || tokenizer.characterReferenceCode in control -> {
                tokenizer.error(ControlCharacterReference(reader.position))
                tokenizer.characterReferenceCode = 0xFFFD
                tokenizer.characterReferenceCode = when (tokenizer.characterReferenceCode) {
                    0x80 -> 0x20AC
                    0x82 -> 0x201A
                    0x83 -> 0x0192
                    0x84 -> 0x201E
                    0x85 -> 0x2026
                    0x86 -> 0x2020
                    0x87 -> 0x2021
                    0x88 -> 0x02C6
                    0x89 -> 0x2030
                    0x8A -> 0x0160
                    0x8B -> 0x2039
                    0x8C -> 0x0152
                    0x8E -> 0x017D
                    0x91 -> 0x2018
                    0x92 -> 0x2019
                    0x93 -> 0x201C
                    0x94 -> 0x201D
                    0x95 -> 0x2022
                    0x96 -> 0x2013
                    0x97 -> 0x2014
                    0x98 -> 0x02DC
                    0x99 -> 0x2122
                    0x9A -> 0x0161
                    0x9B -> 0x203A
                    0x9C -> 0x0153
                    0x9E -> 0x017E
                    0x9F -> 0x0178
                    else -> tokenizer.characterReferenceCode
                }
            }
        }
        tokenizer.createTempBuffer()
        tokenizer.appendToTempBuffer(tokenizer.characterReferenceCode.toChar())
        tokenizer.flushTempBuffer()
        tokenizer.switch(tokenizer.returnState)
    }
}

internal val State.inAttribute get() = this is AttributeValueDoubleQuotedState || this is AttributeValueSingleQuotedState || this is AttributeValueUnquotedState