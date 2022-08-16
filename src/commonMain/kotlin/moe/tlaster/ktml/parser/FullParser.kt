package moe.tlaster.ktml.parser

import moe.tlaster.ktml.dom.Element
import moe.tlaster.ktml.dom.Node
import moe.tlaster.ktml.parser.token.Token


internal class TreeBuilder {
    private var insertionMode = InsertionMode.INITIAL

    // The frameset-ok flag is set to "ok" when the parser is created. It is set to "not ok" after certain tokens are seen.
    private var framesetOk = true
    // The scripting flag is set to "enabled" if scripting was enabled for the Document with which the parser is associated when the parser was created, and "disabled" otherwise.
    private val scriptingEnabled = false

    // https://html.spec.whatwg.org/#the-element-pointers
    private var headerPointer: Node? = null
    private var formPointer: Node? = null

    // Initially, the stack of open elements is empty. The stack grows downwards;
    // the topmost node on the stack is the first one added to the stack,
    // and the bottommost node of the stack is the most recently added node in the stack
    // (notwithstanding when the stack is manipulated in a random access fashion as part of the handling for misnested tags).
    private val stackOfOpenElements = mutableListOf<Element>()


    // https://html.spec.whatwg.org/#the-list-of-active-formatting-elements
    // Initially, the list of active formatting elements is empty. It is used to handle mis-nested formatting element tags.
    private val activeFormattingElements = mutableListOf<Element>()

    private object Marker : Element {
        override val name: String
            get() = "marker"
        override val attributes: Map<String, String>
            get() = emptyMap()
        override val children: List<Node>
            get() = emptyList()
        override val parent: Element?
            get() = null
        override val innerText: String
            get() = ""
        override val namespace: String
            get() = ""
    }

    // The current node is the bottommost node in this stack of open elements.
    private val currentNode get() = stackOfOpenElements.last()

    // The adjusted current node is the context element if the parser was created as part of the HTML fragment parsing algorithm and the stack of open elements has only one element in it (fragment case);
    // otherwise, the adjusted current node is the current node.
    // TODO: implement fragment parsing algorithm
    private val adjustedCurrentNode get() = currentNode

    // Elements in the stack of open elements fall into the following categories:
    private object ElementCategory {
        val special = listOf(
            "address", "applet", "area", "article", "aside", "base", "basefont", "bgsound", "blockquote", "body",
            "br", "button", "caption", "center", "col", "colgroup", "dd", "details", "dir", "div", "dl", "dt",
            "embed", "fieldset", "figcaption", "figure", "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4",
            "h5", "h6", "head", "header", "hgroup", "hr", "html", "iframe", "img", "input", "keygen", "li", "link",
            "listing", "main", "marquee", "menu", "meta", "nav", "noembed", "noframes", "noscript", "object", "ol", "p",
            "param", "plaintext", "pre", "script", "section", "select", "source", "style", "summary", "table", "tbody",
            "td", "template", "textarea", "tfoot", "th", "thead", "title", "tr", "track", "ul", "wbr", "xmp", "mi",
            "mo", "mn", "ms", "mtext", "annotation-xml", "foreignObject", "desc", "title",
        )
        val formatting = listOf(
            "a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u",
        )

        // All other elements found while parsing an HTML document.
        object Ordinary
    }

    // https://html.spec.whatwg.org/#has-an-element-in-the-specific-scope
    // The stack of open elements is said to have an element target node in a specific scope consisting of a list of element types list when the following algorithm terminates in a match state:
    // 1. Initialize node to be the current node (the bottommost node of the stack).
    // 2. If node is the target node, terminate in a match state.
    // 3.Otherwise, if node is one of the element types in list, terminate in a failure state.
    // 4.Otherwise, set node to the previous entry in the stack of open elements and return to step 2.
    // (This will never fail, since the loop will always terminate in the previous step if the top of the stack — an html element — is reached.)
    fun haveAnElementTargetNodeInASpecialScope(
        elementName: String,
        baseTypes: List<String>,
        extraTypes: List<String>,
    ): Boolean {
        for (i in stackOfOpenElements.size - 1 downTo 0) {
            val node = stackOfOpenElements[i]
            if (node.name == elementName) {
                return true
            }
            if (baseTypes.contains(node.name)) {
                return false
            }
            if (extraTypes.contains(node.name)) {
                return false
            }
        }
        return false
    }

    // The stack of open elements is said to have a particular element in scope when it has that element in the specific scope consisting of the following element types:
    //  - applet
    //  - caption
    //  - html
    //  - table
    //  - td
    //  - th
    //  - marquee
    //  - object
    //  - template
    //  - MathML mi
    //  - MathML mo
    //  - MathML mn
    //  - MathML ms
    //  - MathML mtext
    //  - MathML annotation-xml
    //  - SVG foreignObject
    //  - SVG desc
    //  - SVG title
    fun hasAnElementInScope(elementName: String): Boolean {
        return haveAnElementTargetNodeInASpecialScope(
            elementName,
            listOf(
                "applet",
                "caption",
                "html",
                "table",
                "td",
                "th",
                "marquee",
                "object",
                "template",
                "mi",
                "mo",
                "mn",
                "ms",
                "mtext",
                "annotation-xml",
                "foreignObject",
                "desc",
                "title",
            ),
            listOf(),
        )
    }

    // The stack of open elements is said to have a particular element in list item scope when it has that element in the specific scope consisting of the following element types:
    //  - All the element types listed above for the has an element in scope algorithm.
    //  - ol in the HTML namespace
    //  - ul in the HTML namespace
    fun hasAnElementInListItemScope(elementName: String): Boolean {
        return haveAnElementTargetNodeInASpecialScope(
            elementName,
            listOf(
                "applet",
                "caption",
                "html",
                "table",
                "td",
                "th",
                "marquee",
                "object",
                "template",
                "mi",
                "mo",
                "mn",
                "ms",
                "mtext",
                "annotation-xml",
                "foreignObject",
                "desc",
                "title",
            ),
            listOf(
                "ol", "ul",
            ),
        )
    }

    // The stack of open elements is said to have a particular element in button scope when it has that element in the specific scope consisting of the following element types:
    //  - All the element types listed above for the has an element in scope algorithm.
    //  - button in the HTML namespace
    fun hasAnElementInButtonScope(elementName: String): Boolean {
        return haveAnElementTargetNodeInASpecialScope(
            elementName,
            listOf(
                "applet",
                "caption",
                "html",
                "table",
                "td",
                "th",
                "marquee",
                "object",
                "template",
                "mi",
                "mo",
                "mn",
                "ms",
                "mtext",
                "annotation-xml",
                "foreignObject",
                "desc",
                "title",
            ),
            listOf(
                "button",
            ),
        )
    }

    // The stack of open elements is said to have a particular element in table scope when it has that element in the specific scope consisting of the following element types:
    //  - html in the HTML namespace
    //  - table in the HTML namespace
    //  - template in the HTML namespace
    fun hasAnElementInTableScope(elementName: String): Boolean {
        return haveAnElementTargetNodeInASpecialScope(
            elementName,
            listOf(
                "html", "table", "template",
            ),
            listOf(),
        )
    }

    // The stack of open elements is said to have a particular element in select scope when it has that element in the specific scope consisting of all element types except the following:
    //  - optgroup in the HTML namespace
    //  - option in the HTML namespace
    fun hasAnElementInSelectScope(elementName: String): Boolean {
        for (i in stackOfOpenElements.size - 1 downTo 0) {
            val node = stackOfOpenElements[i]
            if (node.name == elementName) {
                return true
            }
            if (node.name == "optgroup") {
                return false
            }
            if (node.name == "option") {
                return false
            }
        }
        return false
    }

    // When the steps below require the UA to push onto the list of active formatting elements an element element, the UA must perform the following steps:
    // 1.If there are already three elements in the list of active formatting elements after the last marker,
    //   if any, or anywhere in the list if there are no markers, that have the same tag name, namespace, and attributes as element,
    //   then remove the earliest such element from the list of active formatting elements.
    //   For these purposes, the attributes must be compared as they were when the elements were created by the parser;
    //   two elements have the same attributes if all their parsed attributes can be paired such that the two attributes in each pair have identical names, namespaces, and values
    //   (the order of the attributes does not matter).
    // 2.Add element to the list of active formatting elements.
    fun pushFormattingElement(element: Element) {
        var numSeen = 0
        for (pos in activeFormattingElements.size - 1 downTo 0) {
            val el: Element = activeFormattingElements[pos]
            if (el is Marker) {
                break
            }
            if (el.name == element.name && el.attributes == element.attributes) {
                numSeen++
            }
            if (numSeen == 3) {
                activeFormattingElements.removeAt(pos)
                break
            }
        }
        activeFormattingElements.add(element)
    }

    // When the steps below require the UA to reconstruct the active formatting elements, the UA must perform the following steps:
    // 1.If there are no entries in the list of active formatting elements, then there is nothing to reconstruct; stop this algorithm.
    // 2.If the last (most recently added) entry in the list of active formatting elements is a marker,
    //   or if it is an element that is in the stack of open elements, then there is nothing to reconstruct; stop this algorithm.
    // 3.Let entry be the last (most recently added) element in the list of active formatting elements.
    // 4.Rewind: If there are no entries before entry in the list of active formatting elements, then jump to the step labeled create.
    // 5.Let entry be the entry one earlier than entry in the list of active formatting elements.
    // 6.If entry is neither a marker nor an element that is also in the stack of open elements, go to the step labeled rewind.
    // 7.Advance: Let entry be the element one later than entry in the list of active formatting elements.
    // 8.Create: Insert an HTML element for the token for which the element entry was created, to obtain new element.
    // 9.Replace the entry for entry in the list with an entry for new element.
    // 10.If the entry for new element in the list of active formatting elements is not the last entry in the list, return to the step labeled advance.
    // This has the effect of reopening all the formatting elements that were opened in the current body, cell, or caption (whichever is youngest) that haven't been explicitly closed.
    fun reconstructTheActiveFormattingElements() {
        // 1.If there are no entries in the list of active formatting elements, then there is nothing to reconstruct.
        if (activeFormattingElements.isEmpty()) {
            return
        }
        // 2.If the last (most recently added) entry in the list of active formatting elements is a marker,
        if (activeFormattingElements.last() is Marker) {
            return
        }
        // or if it is an element that is in the stack of open elements, then there is nothing to reconstruct.
        if (stackOfOpenElements.contains(activeFormattingElements.last())) {
            return
        }
        var skipToAdvance = false
        // 3.Let entry be the last (most recently added) element in the list of active formatting elements.
        var entry = activeFormattingElements.last()
        while (true) {
            if (skipToAdvance) {
                // 7.Advance: Let entry be the element one later than entry in the list of active formatting elements.
                entry = activeFormattingElements[activeFormattingElements.indexOf(entry) + 1]
            } else {
                // 4.Rewind: If there are no entries before entry in the list of active formatting elements, then jump to the step labeled create.
                if (activeFormattingElements.indexOf(entry) != 0) {
                    // 5.Let entry be the entry one earlier than entry in the list of active formatting elements.
                    entry = activeFormattingElements[activeFormattingElements.indexOf(entry) - 1]
                    // 6.If entry is neither a marker nor an element that is also in the stack of open elements, go to the step labeled rewind.
                    if (entry !is Marker && !stackOfOpenElements.contains(entry)) {
                        continue
                    } else {
                        // 7.Advance: Let entry be the element one later than entry in the list of active formatting elements.
                        entry = activeFormattingElements[activeFormattingElements.indexOf(entry) + 1]
                    }
                }
            }
            // 8.Create: Insert an HTML element for the token for which the element entry was created, to obtain new element.
//            val newElement = createElement(entry)
            // 9.Replace the entry for entry in the list with an entry for new element.
//            activeFormattingElements[activeFormattingElements.indexOf(entry)] = newElement
            // 10.If the entry for new element in the list of active formatting elements is not the last entry in the list, return to the step labeled advance.
//            if (activeFormattingElements.indexOf(newElement) != activeFormattingElements.size - 1) {
//                skipToAdvance = true
//                continue
//            }
        }
    }

    // When the steps below require the UA to clear the list of active formatting elements up to the last marker, the UA must perform the following steps:
    // 1.Let entry be the last (most recently added) entry in the list of active formatting elements.
    // 2.Remove entry from the list of active formatting elements.
    // 3.If entry was a marker, then stop the algorithm at this point. The list has been cleared up to the last marker.
    // 4.Go to step 1.
    fun clearTheActiveFormattingElementsUpToTheLastMarker() {
        while (true) {
            val entry = activeFormattingElements.last()
            if (entry is Marker) {
                break
            }
            activeFormattingElements.remove(entry)
        }
    }

    // When the steps below require the user agent to insert an HTML element for a token, the user agent must insert a foreign element for the token, in the HTML namespace.
    fun insertAnHtmlElement(token: Token) {
//        insertForeignElement(token, "http://www.w3.org/1999/xhtml")
    }

    // When the steps below require the user agent to insert a foreign element for a token in a given namespace, the user agent must run these steps:
    // 1.Let the adjusted insertion location be the appropriate place for inserting a node.
    // 2.Let element be the result of creating an element for the token in the given namespace, with the intended parent being the element in which the adjusted insertion location finds itself.
    // 3.If it is possible to insert element at the adjusted insertion location, then:
    // 3.1.If the parser was not created as part of the HTML fragment parsing algorithm, then push a new element queue onto element's relevant agent's custom element reactions stack.
    // 3.2.Insert element at the adjusted insertion location.
    // 3.3.If the parser was not created as part of the HTML fragment parsing algorithm, then pop the element queue from element's relevant agent's custom element reactions stack, and invoke custom element reactions in that queue.
    // 4.Push element onto the stack of open elements so that it is the new current node.
    // 5.Return element.
    fun insertForeignElement(token: Token, namespace: String) {
        val adjustedInsertionLocation = findAppropriateInsertionLocation(token)
        val element = createElement(token, namespace)
        if (adjustedInsertionLocation != null) {
            if (!isParsingFragment) {
                element.relevantAgent.customElementReactionsStack.push(CustomElementReactions(element))
            }
            adjustedInsertionLocation.insert(element)
            if (!isParsingFragment) {
                element.relevantAgent.customElementReactionsStack.pop()
                element.relevantAgent.customElementReactionsStack.peek().invokeCustomElementReactions()
            }
        }
        stackOfOpenElements.push(element)
    }

    // The appropriate place for inserting a node, optionally using a particular override target, is the position in an element returned by running the following steps:
    // 1.If there was an override target specified, then let target be the override target.
    //   Otherwise, let target be the current node.
    // 2.Determine the adjusted insertion location using the first matching steps from the following list:
    //   If foster parenting is enabled and target is a table, tbody, tfoot, thead, or tr element
    //     1.Let last template be the last template element in the stack of open elements, if any.
    //     2.Let last table be the last table element in the stack of open elements, if any.
    //     3.If there is a last template and either there is no last table, or there is one, but last template is lower (more recently added) than last table in the stack of open elements, then: let adjusted insertion location be inside last template's template contents, after its last child (if any), and abort these steps.
    //     4.If there is no last table, then let adjusted insertion location be inside the first element in the stack of open elements (the html element), after its last child (if any), and abort these steps. (fragment case)
    //     5.If last table has a parent node, then let adjusted insertion location be inside last table's parent node, immediately before last table, and abort these steps.
    //     6.Let previous element be the element immediately above last table in the stack of open elements.
    //     7.Let adjusted insertion location be inside previous element, after its last child (if any).
    //  Otherwise
    //     Let adjusted insertion location be inside target, after its last child (if any).
    // 3.If the adjusted insertion location is inside a template element, let it instead be inside the template element's template contents, after its last child (if any).
    // 4.Return the adjusted insertion location.
    fun findAppropriateInsertionLocation(token: Token): Node? {
        var target = if (overrideTarget != null) overrideTarget else currentNode
        if (fosterParenting && target is Element && (target.tagName == "table" || target.tagName == "tbody" || target.tagName == "tfoot" || target.tagName == "thead" || target.tagName == "tr")) {
            val lastTemplate = stackOfOpenElements.last { it is Template } as Template
            val lastTable = stackOfOpenElements.last { it is Element && it.tagName == "table" } as Element?
            if (lastTemplate != null && (lastTable == null || lastTemplate.indexInStack < lastTable.indexInStack)) {
                return lastTemplate.templateContents.lastChild?.let { it.nextSibling } ?: lastTemplate.templateContents
            }
            if (lastTable == null) {
                return stackOfOpenElements.first { it is Element }.lastChild?.let { it.nextSibling } ?: stackOfOpenElements.first { it is Element }
            }
            if (lastTable.parentNode != null) {
                return lastTable.parentNode
            }
            val previousElement = stackOfOpenElements.get(lastTable.indexInStack - 1)
            return previousElement.lastChild?.let { it.nextSibling } ?: previousElement
        } else {
            return target.lastChild?.let { it.nextSibling } ?: target
        }
    }

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