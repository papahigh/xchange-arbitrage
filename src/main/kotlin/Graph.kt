package com.github.papahigh

import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.ln


data class Edge(val from: Int, val to: Int, val price: Double) {
    val weight = -ln(price)

    override fun toString(): String = "Edge($from -> $to, price=$price, weight=$weight)"
}

class Graph(private val vertices: Int) {

    private val graph = Array<MutableList<Edge>>(vertices, init = { mutableListOf() })

    fun addEdge(edge: Edge) = graph[edge.from].add(edge)

    fun adjacent(vertex: Int): List<Edge> = graph[vertex]

    fun vertices(): Int = vertices

}

class DijkstraPrice(private val graph: Graph, source: Int) {

    private val distTo = Array(graph.vertices()) { Double.POSITIVE_INFINITY }
    private val edgeTo = Array<Edge?>(graph.vertices()) { null }
    private val marked = Array(graph.vertices()) { false }
    private val queue = PriorityQueue<Element>()

    init {
        distTo[source] = 0.0
        queue.add(Element(source, distTo[source]))

        while (!queue.isEmpty()) {
            var curr = queue.poll()
            marked[curr.vertex] = true
            relax(curr.vertex)
        }
    }

    private fun relax(v: Int) {
        for (edge in graph.adjacent(v)) {
            val w = edge.to
            if (marked[w]) continue
            if (distTo[w] > distTo[v] + edge.price) {
                distTo[w] = distTo[v] + edge.price
                edgeTo[w] = edge
                queue.offer(Element(w, distTo[w]))
            }
        }
    }

    data class Element(val vertex: Int, val price: Double) : Comparable<Element> {
        override fun compareTo(other: Element): Int = this.price.compareTo(other.price)
    }

    fun distTo(vertex: Int): Double = distTo[vertex]

    fun hasPathTo(vertex: Int): Boolean = marked[vertex]

    fun shortestPathTo(vertex: Int): Iterable<Edge> {
        val path = LinkedList<Edge>()
        var edge = edgeTo[vertex]
        while (edge != null) {
            path.push(edge)
            edge = edgeTo[edge.from]
        }
        return path
    }
}

class BellmanFordWeight(private val graph: Graph, private val source: Int) {

    private val distTo = Array(graph.vertices()) { Double.POSITIVE_INFINITY }
    private val edgeTo = Array<Edge?>(graph.vertices()) { null }

    private val queue = ArrayDeque<Int>()
    private val onQueue = Array(graph.vertices()) { false }

    private var cyclePath: Iterable<Edge>? = null
    private var enterPath: Iterable<Edge>? = null
    private var counter = 0

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

    fun negativeCyclePath(): Iterable<Edge>? = cyclePath

    fun negativeEnterPath(): Iterable<Edge>? = enterPath

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
            if (distTo[w] > distTo[v] + edge.weight) {
                distTo[w] = distTo[v] + edge.weight
                edgeTo[w] = edge
                if (!onQueue[w]) {
                    queue.addLast(w)
                    onQueue[w] = true
                }
            }
            if (counter++ % graph.vertices() == 0 && counter > 1) {
                findNegativeCycle()
            }
        }
    }

    private fun findNegativeCycle() {
        var curr = Graph(graph.vertices())
        for (v in 0 until graph.vertices()) {
            edgeTo[v]?.let { curr.addEdge(it) }
        }
        CycleFinder(curr, graph, source).let {
            cyclePath = it.cyclePath()
            enterPath = it.enterPath()
        }
    }
}


class CycleFinder(
    private val targetGraph: Graph,
    private val originalGraph: Graph,
    private val source: Int
) {

    constructor(graph: Graph, source: Int) : this(graph, graph, source)

    private val edgeTo = Array<Edge?>(targetGraph.vertices()) { null }
    private val onStack = Array(targetGraph.vertices()) { false }
    private val marked = Array(targetGraph.vertices()) { false }

    private var enterPath: Iterable<Edge>? = null
    private var cyclePath: Iterable<Edge>? = null

    init {
        for (v in 0 until targetGraph.vertices())
            if (!marked[v])
                dfs(v)
    }

    fun enterPath(): Iterable<Edge>? = enterPath

    fun cyclePath(): Iterable<Edge>? = cyclePath

    fun hasCycle(): Boolean = cyclePath != null

    private fun dfs(vertex: Int) {
        onStack[vertex] = true
        marked[vertex] = true

        for (edge in targetGraph.adjacent(vertex)) {
            if (hasCycle()) return
            if (!marked[edge.to]) {
                edgeTo[edge.to] = edge
                dfs(edge.to)
            } else if (onStack[edge.to]) {
                addCycle(vertex, edge)
            }
        }

        onStack[vertex] = false
    }

    private fun addCycle(v: Int, edge: Edge) {
        var cycle = LinkedList<Edge>()
        var i = v
        do {
            cycle.push(edgeTo[i])
            i = edgeTo[i]!!.from
        } while (i != edge.to)
        cycle.push(edge)

        val shortestPathTree = DijkstraPrice(originalGraph, source)
        var minDistance = Double.POSITIVE_INFINITY
        var cycleStart = -1

        for (e in cycle) {
            if (shortestPathTree.distTo(e.from) < minDistance) {
                minDistance = shortestPathTree.distTo(e.from)
                cycleStart = e.from
            }
        }

        enterPath = if (cycleStart >= 0) {
            var enter = shortestPathTree.shortestPathTo(cycleStart)
            while (cycle.first().from != cycleStart) {
                cycle.addLast(cycle.removeFirst())
            }
            enter
        } else emptyList()

        cyclePath = cycle

        println("enter: $enterPath")
        println("cycle: $cyclePath")
    }
}