package moe.tlaster.ktml.parser

internal const val eof: Char = (-1).toChar()

internal interface Reader {
    val position: Long
    fun consume(length: Int = 1): Char
    fun next(): Char
    fun hasNext(): Boolean
    fun pushback(c: Char)
    fun isFollowedBy(value: String, ignoreCase: Boolean = false): Boolean
}

internal class StringReader(private val string: String) : Reader {
    override val position: Long
        get() = _position
    private var _position = 0L
    override fun consume(length: Int): Char {
        val c = string[_position.toInt()]
        _position += length
        return c
    }
    override fun next(): Char {
        val c = string[_position.toInt()]
        _position++
        return c
    }
    override fun hasNext(): Boolean {
        return _position < string.length
    }
    override fun pushback(c: Char) {
        _position--
    }
    override fun isFollowedBy(value: String, ignoreCase: Boolean): Boolean {
        val length = value.length
        val end = _position + length
        if (end > string.length) {
            return false
        }
        val s = string.substring(_position.toInt(), end.toInt())
        return if (ignoreCase) s.equals(value, ignoreCase = true) else s == value
    }
}
