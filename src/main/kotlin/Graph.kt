package com.github.papahigh

import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.abs


interface Edge {
    val from: Int
    val to: Int
}

class Graph<E : Edge>(private val vertexCount: Int) {

    private val graph = Array<MutableList<E>>(vertexCount, init = { mutableListOf() })
    private var edgeCount = 0

    fun addEdge(edge: E) {
        graph[edge.from].add(edge)
        edgeCount++
    }

    fun adjacent(vertex: Int): List<E> = graph[vertex]
    fun vertexCount(): Int = vertexCount
    fun edgeCount(): Int = edgeCount
}


class SymbolGraph<V, E : Edge> private constructor(
    private val symbolTable: Map<V, Int>,
    private val symbolIndex: List<V>
) {

    val graph = Graph<E>(symbolTable.size)

    fun addEdge(edge: E) = graph.addEdge(edge)

    fun symbolOf(i: Int): V? {
        if (i < 0 || i >= symbolTable.size) return null
        return symbolIndex[i]
    }

    fun indexOf(symbol: V): Int? = symbolTable[symbol]
    fun contains(symbol: V): Boolean = symbolTable.containsKey(symbol)

    companion object {

        fun <V, E : Edge> of(symbols: Collection<V>): SymbolGraph<V, E> {
            val table = mutableMapOf<V, Int>()
            val index: MutableList<V> = ArrayList()

            for (i in symbols) {
                if (!table.containsKey(i)) {
                    index.add(i)
                    table[i] = table.size
                }
            }

            return SymbolGraph<V, E>(table, index)
        }
    }
}

interface WeightedEdge : Edge {
    val weight: Double
}

class BellmanFord<E : WeightedEdge> private constructor(
    private val graph: Graph<E>,
    private val source: Int
) {

    private val distTo = Array(graph.vertexCount()) { Double.POSITIVE_INFINITY }

    private var edgeTo = arrayOfNulls<Edge>(graph.vertexCount())
    private val queue = ArrayDeque<Int>()

    private val onQueue = Array(graph.vertexCount()) { false }
    private var cyclePath: Iterable<Edge>? = null
    private var iteration = 0

    init {
        distTo[source] = 0.0
        queue.addLast(source)
        onQueue[source] = true

        while (!queue.isEmpty() && !hasNegativeCycle()) {
            val v = queue.removeFirst()
            onQueue[v] = false
            relax(v)
        }
    }

    companion object {
        fun <E : WeightedEdge> of(graph: Graph<E>, source: Int): BellmanFord<E> {
            return BellmanFord<E>(graph, source)
        }

        fun <V, E : WeightedEdge> of(symbolGraph: SymbolGraph<V, E>, sourceSymbol: V): BellmanFord<E> {
            return BellmanFord<E>(symbolGraph.graph, symbolGraph.indexOf(sourceSymbol)!!)
        }
    }

    fun negativeCyclePath(): Iterable<Edge>? = cyclePath

    fun hasNegativeCycle(): Boolean = cyclePath != null

    fun shortestPathTo(vertex: Int): Iterable<Edge> {
        if (!hasNegativeCycle() && distTo[vertex].isFinite()) {
            val path = LinkedList<Edge>()
            var edge = edgeTo[vertex]
            while (edge != null) {
                path.push(edge)
                edge = edgeTo[edge.from]
            }
            return path
        }
        return emptyList()
    }

    private fun relax(v: Int) {
        for (edge in graph.adjacent(v)) {
            val w = edge.to
            if (distTo[w].greaterThan(distTo[v] + edge.weight)) {
                distTo[w] = distTo[v] + edge.weight
                edgeTo[w] = edge
                if (!onQueue[w]) {
                    queue.addLast(w)
                    onQueue[w] = true
                }
            }
            if (iteration++ % graph.vertexCount() == 0) {
                findNegativeCycle()
            }
        }
    }

    private fun findNegativeCycle() {
        var curr = Graph<Edge>(graph.vertexCount())
        for (v in 0 until graph.vertexCount()) {
            edgeTo[v]?.let { curr.addEdge(it) }
        }
        CycleFinder(curr).let {
            cyclePath = it.cyclePath()
        }
    }
}

const val EPS = 1e-6

fun Double.greaterThan(other: Double): Boolean {
    var that = this.toDouble()
    if (abs(that - other) < EPS) return false
    return that > other
}


class CycleFinder<E : Edge>(private val graph: Graph<E>) {

    private val marked = Array(graph.vertexCount()) { 0 }
    private val edgeTo = Array<Edge?>(graph.vertexCount()) { null }
    private var cyclePath: Collection<Edge>? = null

    init {
        for (v in 0 until graph.vertexCount())
            if (marked[v] == 0)
                dfs(v)
    }

    fun cyclePath(): Collection<Edge>? = cyclePath

    fun hasCycle(): Boolean = cyclePath != null

    private fun dfs(vertex: Int) {
        marked[vertex] += 2

        for (edge in graph.adjacent(vertex)) {
            if (hasCycle()) return
            if (marked[edge.to] == 0) {
                edgeTo[edge.to] = edge
                dfs(edge.to)
            } else if (marked[edge.to] > 1) {
                addCycle(vertex, edge)
            }
        }

        marked[vertex] -= 1
    }

    private fun addCycle(v: Int, edge: Edge) {
        var cycle = LinkedList<Edge>()
        var i = v
        do {
            cycle.push(edgeTo[i])
            i = edgeTo[i]!!.from
        } while (i != edge.to)
        cycle.push(edge)
        cyclePath = cycle
    }
}
