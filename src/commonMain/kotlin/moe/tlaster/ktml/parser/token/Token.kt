package moe.tlaster.ktml.parser.token

import kotlin.Char

internal sealed interface Token

internal object EOF : Token
internal data class Character(val c: Char) : Token
internal object TagOpen : Token
internal data class Tag(val char: Char) : Token
internal object TagClose : Token
internal object TagSelfClose : Token
internal object EndTagOpen : Token
internal object CommentOpen : Token
internal data class Comment(val char: Char) : Token
internal object CommentClose : Token
internal object AttributeOpen : Token
internal data class Attribute(val char: Char) : Token
internal data class AttributeValue(val char: Char) : Token
internal object DocTypeOpen : Token
internal data class DocType(val char: Char) : Token
internal data class DocTypePublicIdentifier(val char: Char) : Token
internal data class DocTypeSystemIdentifier(val char: Char) : Token
internal object DocTypeClose : Token
internal object ForceQuirks : Token
internal object CharacterReferenceOpen : Token
internal data class CharacterReferenceCodeMultiples(val by: Int) : Token
internal data class CharacterReferenceCodeAdd(val value: Int) : Token
internal object CharacterReferenceClose : Token