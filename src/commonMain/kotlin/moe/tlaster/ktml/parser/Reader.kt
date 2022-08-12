package moe.tlaster.ktml.parser

internal const val eof: Char = (-1).toChar()

internal sealed interface Reader {
    val position: Int
    fun consume(): Char
    fun consume(length: Int): String
    fun next(): Char
    fun hasNext(): Boolean
    fun pushback(length: Int = 1)
    fun isFollowedBy(value: String, ignoreCase: Boolean = false): Boolean
}

internal class StringReader(string: String) : Reader {
    private val string: String
    override val position: Int
        get() = _position
    private var _position = 0

    override fun consume(): Char {
        val c = string[_position]
        _position++
        return c
    }
    override fun consume(length: Int): String {
        val s = string.substring(_position, _position + length)
        _position += length
        return s
    }

    override fun next(): Char {
        return string[_position]
    }
    override fun hasNext(): Boolean {
        return _position < string.length
    }
    override fun pushback(length: Int) {
        _position -= length
    }
    override fun isFollowedBy(value: String, ignoreCase: Boolean): Boolean {
        val length = value.length
        val end = _position + length
        if (end > string.length) {
            return false
        }
        val s = string.substring(_position, end)
        return if (ignoreCase) s.equals(value, ignoreCase = true) else s == value
    }

    init {
        this.string = string + eof
    }
}
