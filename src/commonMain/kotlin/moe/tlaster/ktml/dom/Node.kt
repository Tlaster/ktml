package moe.tlaster.ktml.dom

data class Attribute(
    val name: String,
    val value: String?,
    val namespace: String? = null
) {
    override fun toString(): String {
        return "$name${if (value == null) "" else "=\"$value\""}"
    }
}

interface Node {
    val name: String
    val namespace: String?
}

interface Element : Node {
    val attributes: List<Attribute>
    val children: List<Node>
    val parent: Element?
    val innerHTML: String
    val outerHTML: String
    val innerText: String
}

data class HtmlElement(
    override val name: String,
    override val namespace: String? = null,
) : Element {
    override val attributes = arrayListOf<Attribute>()
    override val children = arrayListOf<Node>()
    override var parent: Element? = null
    override var innerHTML: String = ""
    override val outerHTML: String
        get() = "<$name${attributes.joinToString(" ") { it.toString() }}>$innerHTML</$name>"
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
    override val name: String = "#text",
    override val namespace: String? = null,
    val text: String
) : Node {
    override fun toString(): String {
        return text
    }
}

data class Comment(
    override val name: String = "#comment",
    override val namespace: String? = null,
    val text: String
) : Node {
    override fun toString(): String {
        return "<!--$text-->"
    }
}

data class Document(
    override val name: String = "#document",
    override val namespace: String? = null,
    val children: List<Node>
) : Node {
    override fun toString(): String {
        return children.joinToString("")
    }
}

data class DocumentType(
    override val name: String,
    override val namespace: String? = null,
) : Node {
    override fun toString(): String {
        return "<!DOCTYPE $name>"
    }
}

data class CDATASection(
    override val name: String = "#cdataSection",
    override val namespace: String? = null,
    val text: String
) : Node {
    override fun toString(): String {
        return "<![CDATA[$text]]>"
    }
}