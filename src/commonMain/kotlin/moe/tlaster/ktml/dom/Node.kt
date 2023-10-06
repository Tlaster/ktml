package moe.tlaster.ktml.dom

sealed interface Node {
    val name: String
}

data class Element(
    override val name: String,
    val namespace: String = "",
    val parent: Element? = null,
) : Node {
    val attributes = linkedMapOf<String, String>()
    val children = arrayListOf<Node>()
    val innerText: String
        get() = children.joinToString("") {
            when (it) {
                is Text -> it.text
                is Element -> it.innerText
                else -> ""
            }
        }
}

data class Text(
    val text: String,
    override val name: String = "#text",
) : Node {
    override fun toString(): String {
        return text
    }
}

data class Comment(
    val text: String,
    override val name: String = "#comment",
) : Node {
    override fun toString(): String {
        return "<!--$text-->"
    }
}

data class Doctype(
    override val name: String,
) : Node {
    override fun toString(): String {
        return "<!DOCTYPE $name>"
    }
}
