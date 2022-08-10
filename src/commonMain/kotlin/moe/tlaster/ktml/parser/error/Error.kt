package moe.tlaster.ktml.parser.error

internal sealed interface Error {
    val position: Long
}

internal sealed interface ParserError : Error
internal data class UnexpectedNullCharacterError(override val position: Long) : ParserError
internal data class UnexpectedQuestionMarkInsteadOfTagName(override val position: Long) : ParserError
internal data class InvalidFirstCharacterOfTagName(override val position: Long) : ParserError
internal data class MissingEndTagName(override val position: Long) : ParserError
internal data class EndBeforeTagName(override val position: Long) : ParserError
internal data class EofInTag(override val position: Long) : ParserError
internal data class EofInScriptHtmlCommentLikeText(override val position: Long) : ParserError
internal data class UnexpectedEqualsSignBeforeAttributeName(override val position: Long) : ParserError
internal data class UnexpectedCharacterInAttributeName(override val position: Long) : ParserError
internal data class MissingAttributeValue(override val position: Long) : ParserError
internal data class UnexpectedCharacterInUnquotedAttributeValue(override val position: Long) : ParserError
internal data class MissingWhitespaceBetweenAttributes(override val position: Long) : ParserError
internal data class UnexpectedSolidusInTag(override val position: Long) : ParserError
internal data class IncorrectlyOpenedComment(override val position: Long) : ParserError
internal data class IncorrectlyClosedComment(override val position: Long) : ParserError
internal data class AbruptClosingOfEmptyComment(override val position: Long) : ParserError
internal data class EofInComment(override val position: Long) : ParserError
internal data class NestedComment(override val position: Long) : ParserError
internal data class EofInDoctype(override val position: Long) : ParserError
internal data class MissingWhitespaceBeforeDoctypeName(override val position: Long) : ParserError
internal data class MissingDoctypeName(override val position: Long) : ParserError
internal data class InvalidCharacterSequenceAfterDoctypeName(override val position: Long) : ParserError
internal data class MissingWhitespaceAfterDoctypePublicKeyword(override val position: Long) : ParserError
internal data class MissingWhitespaceAfterDoctypeSystemKeyword(override val position: Long) : ParserError
internal data class MissingDoctypePublicIdentifier(override val position: Long) : ParserError
internal data class MissingDoctypeSystemIdentifier(override val position: Long) : ParserError
internal data class MissingQuoteBeforeDoctypePublicIdentifier(override val position: Long) : ParserError
internal data class MissingQuoteBeforeDoctypeSystemIdentifier(override val position: Long) : ParserError
internal data class AbruptDoctypePublicIdentifier(override val position: Long) : ParserError
internal data class AbruptDoctypeSystemIdentifier(override val position: Long) : ParserError
internal data class MissingWhitespaceBetweenDoctypePublicAndSystemIdentifiers(override val position: Long) : ParserError
internal data class UnexpectedCharacterAfterDoctypeSystemIdentifier(override val position: Long) : ParserError
internal data class EofInCdata(override val position: Long) : ParserError
internal data class MissingSemicolonAfterCharacterReference(override val position: Long) : ParserError
internal data class UnkonwnNamedCharacterReference(override val position: Long) : ParserError
internal data class AbsenceOfDigitInNumericCharacterReference(override val position: Long) : ParserError
internal data class NullCharacterReference(override val position: Long) : ParserError
internal data class CharacterReferenceOutOfRange(override val position: Long) : ParserError
internal data class CharacterReferenceSurrogate(override val position: Long) : ParserError
internal data class NonCharacterCharacterReference(override val position: Long) : ParserError
internal data class ControlCharacterReference(override val position: Long) : ParserError