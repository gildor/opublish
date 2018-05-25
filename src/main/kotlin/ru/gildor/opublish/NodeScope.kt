package ru.gildor.opublish

import groovy.util.Node

/**
 * Helper DSL to define Pom
 */
class NodeScope(private val node: Node, block: NodeScope.() -> Unit) {
    init {
        block()
    }

    infix fun String.to(value: String?) {
        if (value != null) {
            node.appendNode(this, value)
        }
    }

    operator fun String.invoke(block: NodeScope.() -> Unit) {
        node.appendNode(this).apply { NodeScope(this, block) }
    }
}