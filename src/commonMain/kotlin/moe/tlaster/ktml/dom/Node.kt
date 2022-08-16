package moe.tlaster.ktml.dom

interface Node {
    val name: String
}

interface Element : Node {
    val attributes: Map<String, String>
    val children: List<Node>
    val parent: Element?
//    val innerHTML: String
//    val outerHTML: String
    val innerText: String
    val namespace: String
}

data class HtmlElement(
    override val name: String,
    override val namespace: String = "",
    override val parent: Element? = null,
) : Element {
    override val attributes = linkedMapOf<String, String>()
    override val children = arrayListOf<Node>()
//    override var innerHTML: String = ""
//    override val outerHTML: String
//        get() = "<$name${attributes.map { "${it.key}=${it.value}" }.joinToString(" ")}>$innerHTML</$name>"
    override val innerText: String
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
