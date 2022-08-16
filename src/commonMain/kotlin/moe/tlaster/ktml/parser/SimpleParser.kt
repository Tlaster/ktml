package moe.tlaster.ktml.parser

import moe.tlaster.ktml.dom.HtmlElement
import moe.tlaster.ktml.dom.Node
import moe.tlaster.ktml.parser.token.Attribute
import moe.tlaster.ktml.parser.token.Comment
import moe.tlaster.ktml.parser.token.Doctype
import moe.tlaster.ktml.parser.token.EOF
import moe.tlaster.ktml.parser.token.EndTag
import moe.tlaster.ktml.parser.token.Tag
import moe.tlaster.ktml.parser.token.Text
import moe.tlaster.ktml.parser.token.Token

internal object SimpleParser {
    private val voidElements = listOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
    fun parse(tokens: List<Token>): Node {
        val stack = mutableListOf<Node>()
        val root = HtmlElement("root")
        var current = root
        stack.add(root)
        for (token in tokens) {
            when (token) {
                is Attribute -> {
                    current.attributes[token.name] = token.value
                }
                is Comment -> {
                    val element = stack.last()
                    if (element is HtmlElement) {
                        element.children.add(moe.tlaster.ktml.dom.Comment(token.text))
                    }
                }
                is Doctype -> {
                    val element = stack.last()
                    if (element is HtmlElement) {
                        element.children.add(moe.tlaster.ktml.dom.Doctype(token.text))
                    }
                }
                EOF -> {
                    // do nothing
                }
                is Tag -> {
                    val element = stack.last()
                    if (element is HtmlElement) {
                        val tag = HtmlElement(token.name, parent = element)
                        element.children.add(tag)
                        if (token.name !in voidElements) {
                            stack.add(tag)
                        }
                        current = tag
                    }
                }
                is EndTag -> {
                    val element = stack.last()
                    if (element is HtmlElement) {
                        if (element.name == token.name) {
                            stack.removeAt(stack.size - 1)
                            current = stack.last() as HtmlElement
                        } else {
                            throw Exception("unmatch tag ${element.name} and ${token.name}")
                        }
                    }
                }
                is Text -> {
                    val element = stack.last()
                    if (element is HtmlElement) {
                        element.children.add(moe.tlaster.ktml.dom.Text(token.text))
                    }
                }
            }
        }
        return root
    }
}