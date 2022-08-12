package moe.tlaster.ktml.parser.token

import kotlin.Char

internal sealed interface TokenCharacter

internal sealed interface TokenBuilder {
    fun build(): Token
}

internal sealed interface Token
internal data class Text(val text: String) : Token
internal data class Tag(val name: String) : Token
internal data class EndTag(val name: String) : Token
internal data class Attribute(val name: String, val value: String) : Token
internal data class Comment(val text: String) : Token
internal data class Doctype(val text: String, val publicIdentifier: String, val systemIdentifier: String, val forceQuirks: Boolean) : Token

internal object EOF : TokenCharacter, Token

internal data class Character(val c: Char) : TokenCharacter
internal data class TextBuilder(val content: StringBuilder = StringBuilder()): TokenBuilder {
    override fun build(): Token {
        return Text(content.toString())
    }
}

internal object TagOpen : TokenCharacter
internal data class TagChar(val char: Char) : TokenCharacter
internal data class TagBuilder(val name: StringBuilder = StringBuilder()) : TokenBuilder {
    override fun build(): Token {
        return Tag(name.toString())
    }
}

internal data class EndTagBuilder(val name: StringBuilder = StringBuilder()) : TokenBuilder {
    override fun build(): Token {
        return EndTag(name.toString())
    }
}

internal object TagClose : TokenCharacter
internal object TagSelfClose : TokenCharacter
internal object EndTagOpen : TokenCharacter
internal object CommentOpen : TokenCharacter
internal data class CommentChar(val char: Char) : TokenCharacter
internal data class CommentBuilder(val content: StringBuilder = StringBuilder()) : TokenBuilder {
    override fun build(): Token {
        return Comment(content.toString())
    }
}

internal object CommentClose : TokenCharacter
internal object AttributeOpen : TokenCharacter
internal data class AttributeChar(val char: Char) : TokenCharacter
internal data class AttributeBuilder(val name: StringBuilder = StringBuilder(), val value: StringBuilder = StringBuilder()) : TokenBuilder {
    override fun build(): Token {
        return Attribute(name.toString(), value.toString())
    }
}

internal data class AttributeValueChar(val char: Char) : TokenCharacter
internal object DocTypeOpen : TokenCharacter
internal data class DocTypeChar(val char: Char) : TokenCharacter
internal data class DocTypeBuilder(val name: StringBuilder = StringBuilder(), val publicIdentifier: StringBuilder = StringBuilder(), val systemIdentifier: StringBuilder = StringBuilder(), var forceQuirks: Boolean = false) : TokenBuilder {
    override fun build(): Token {
        return Doctype(name.toString(), publicIdentifier.toString(), systemIdentifier.toString(), forceQuirks)
    }
}

internal data class DocTypePublicIdentifierChar(val char: Char) : TokenCharacter
internal data class DocTypeSystemIdentifierChar(val char: Char) : TokenCharacter
internal object DocTypeClose : TokenCharacter
internal object ForceQuirks : TokenCharacter
internal object CharacterReferenceOpen : TokenCharacter
internal data class CharacterReferenceCodeMultiples(val by: Int) : TokenCharacter
internal data class CharacterReferenceCodeAdd(val value: Int) : TokenCharacter
internal object CharacterReferenceClose : TokenCharacter