package moe.tlaster.ktml.parser

import moe.tlaster.ktml.dom.Element
import moe.tlaster.ktml.parser.token.Token

internal class Parser(
    private val tokens: List<Token>
) {
}

internal class TreeBuilder {
    private var insertionMode = InsertionMode.INITIAL
    private val stackOfOpenElements = mutableListOf<Element>()

    /*
    When the steps below require the UA to reset the insertion mode appropriately, it means the UA must follow these steps:
        Let last be false.
        Let node be the last node in the stack of open elements.
        Loop: If node is the first node in the stack of open elements, then set last to true, and, if the parser was created as part of the HTML fragment parsing algorithm (fragment case), set node to the context element passed to that algorithm.
        If node is a select element, run these substeps:
            If last is true, jump to the step below labeled done.
            Let ancestor be node.
            Loop: If ancestor is the first node in the stack of open elements, jump to the step below labeled done.
            Let ancestor be the node before ancestor in the stack of open elements.
            If ancestor is a template node, jump to the step below labeled done.
            If ancestor is a table node, switch the insertion mode to "in select in table" and return.
            Jump back to the step labeled loop.
            Done: Switch the insertion mode to "in select" and return.
        If node is a td or th element and last is false, then switch the insertion mode to "in cell" and return.
        If node is a tr element, then switch the insertion mode to "in row" and return.
        If node is a tbody, thead, or tfoot element, then switch the insertion mode to "in table body" and return.
        If node is a caption element, then switch the insertion mode to "in caption" and return.
        If node is a colgroup element, then switch the insertion mode to "in column group" and return.
        If node is a table element, then switch the insertion mode to "in table" and return.
        If node is a template element, then switch the insertion mode to the current template insertion mode and return.
        If node is a head element and last is false, then switch the insertion mode to "in head" and return.
        If node is a body element, then switch the insertion mode to "in body" and return.
        If node is a frameset element, then switch the insertion mode to "in frameset" and return. (fragment case)
        If node is an html element, run these substeps:
            If the head element pointer is null, switch the insertion mode to "before head" and return. (fragment case)
            Otherwise, the head element pointer is not null, switch the insertion mode to "after head" and return.
        If last is true, then switch the insertion mode to "in body" and return. (fragment case)
        Let node now be the node before node in the stack of open elements.
        Return to the step labeled loop.
     */
    private fun resetInsertionModeAppropriately() {
        // Let last be false.
        var last = false
        // Let node be the last node in the stack of open elements.
        val node = stackOfOpenElements.last()
        // Loop: If node is the first node in the stack of open elements, then set last to true, and, if the parser was created as part of the HTML fragment parsing algorithm (fragment case), set node to the context element passed to that algorithm.

    }
}

enum class InsertionMode {
    INITIAL,
    BEFORE_HTML,
    BEFORE_HEAD,
    IN_HEAD,
    IN_HEAD_NOSCRIPT,
    AFTER_HEAD,
    IN_BODY,
    TEXT,
    IN_TABLE,
    IN_TABLE_TEXT,
    IN_CAPTION,
    IN_COLUMN_GROUP,
    IN_TABLE_BODY,
    IN_ROW,
    IN_CELL,
    IN_SELECT,
    IN_SELECT_INTABLE,
    IN_SELECT_INTABLE_TEXT,
    IN_TEMPLATE,
    AFTER_BODY,
    IN_FRAMESET,
    AFTER_FRAMESET,
    AFTER_AFTER_BODY,
    AFTER_AFTER_FRAMESET
}