import com.github.papahigh.*
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.expect


class SymbolGraphTest {

    private val sg = SymbolGraph.of<String, E>(listOf("A", "B", "C", "D", "E", "F"))

    @Test
    fun testSymbolGraph() {
        addEdge(E("A", "B"))
        addEdge(E("A", "C"))
        addEdge(E("B", "D"))
        addEdge(E("C", "E"))
        addEdge(E("C", "F"))
        addEdge(E("F", "E"))

        expect(6) { sg.graph.edgeCount() }
        expect(6) { sg.graph.vertexCount() }

        assertNeighbours("A", listOf("B", "C"))
        assertNeighbours("B", listOf("D"))
        assertNeighbours("C", listOf("E", "F"))
        assertNeighbours("D", listOf())
        assertNeighbours("E", listOf())
        assertNeighbours("F", listOf("E"))
    }

    private fun assertNeighbours(vertex: String, neighbours: List<String>) {
        var adjacent = sg.graph.adjacent(sg.indexOf(vertex)!!)
            .map { it.to }
            .map { sg.symbolOf(it) }

        expect(neighbours) { adjacent }
    }

    private fun addEdge(edge: E) {
        sg.addEdge(edge)
    }

    private inner class E(override val from: Int, override val to: Int) : Edge {
        constructor(from: String, to: String) : this(sg.indexOf(from)!!, sg.indexOf(to)!!)
    }
}

class BellmanFordTest {

    private val sg = SymbolGraph.of<String, E>(listOf("USD", "EUR", "GBP", "CHF", "CAD"))

    @Test
    fun testArbitrageFound() {

        addEdges(
            E("USD", "EUR", 0.741),
            E("USD", "GBP", 0.657),
            E("USD", "CHF", 1.061),
            E("USD", "CAD", 1.005),
            E("EUR", "USD", 1.349),
            E("EUR", "GBP", 0.888),
            E("EUR", "CHF", 1.433),
            E("EUR", "CAD", 1.366),
            E("GBP", "USD", 1.521),
            E("GBP", "EUR", 1.126),
            E("GBP", "CHF", 1.614),
            E("GBP", "CAD", 1.538),
            E("CHF", "USD", 0.942),
            E("CHF", "EUR", 0.698),
            E("CHF", "GBP", 0.619),
            E("CHF", "CAD", 0.953),
            E("CAD", "USD", 0.995),
            E("CAD", "EUR", 0.732),
            E("CAD", "GBP", 0.650),
            E("CAD", "CHF", 0.049),
        )

        var target = BellmanFord.of(sg, "USD")

        assertThat(
            target,
            hasCycle = true,
            cyclePath = listOf(E("GBP", "USD", 1.521), E("USD", "EUR", 0.741), E("EUR", "GBP", 0.888))
        )
    }

    private fun assertThat(target: BellmanFord<E>, hasCycle: Boolean, cyclePath: List<E>? = null) {
        expect(hasCycle) { target.hasNegativeCycle() }
        expect(cyclePath) { target.negativeCyclePath() }
    }

    private fun addEdges(vararg edges: E) {
        for (edge in edges)
            sg.addEdge(edge)
    }

    inner class E(override val from: Int, override val to: Int, override val weight: Double) : WeightedEdge {
        constructor(from: String, to: String, price: Double) : this(
            sg.indexOf(from)!!,
            sg.indexOf(to)!!,
            -ln(price)
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is E) return false

            if (from != other.from) return false
            if (to != other.to) return false
            if (weight != other.weight) return false

            return true
        }

        override fun hashCode(): Int {
            var result = from
            result = 31 * result + to
            result = 31 * result + weight.hashCode()
            return result
        }

        override fun toString(): String {
            return "E(from=$from, to=$to, weight=$weight)"
        }
    }
}


class CycleFinderTest {

    @Test
    fun testCycleFound1() {
        val graph = createGraph(
            7,
            E(0, 1),
            E(0, 2),
            E(1, 2), // 1 -> 2
            E(1, 4),
            E(2, 5), // 2 -> 5
            E(3, 1), // 3 -> 1
            E(3, 4),
            E(4, 6),
            E(5, 3), // 5 -> 3
            E(5, 4),
            E(5, 6),
        )

        assertThat(CycleFinder(graph), hasCycle = true, cyclePath = listOf(E(3, 1), E(1, 2), E(2, 5), E(5, 3)))
    }

    @Test
    fun testCycleFound2() {
        val graph = createGraph(
            7,
            E(0, 1), // 0 -> 1
            E(0, 2),
            E(1, 4), // 1 -> 4
            E(1, 3),
            E(2, 0), // 2 -> 0
            E(2, 3),
            E(2, 5),
            E(3, 4),
            E(3, 5),
            E(4, 2), // 4 -> 2
            E(4, 6),
            E(5, 4),
            E(5, 6),
        )

        assertThat(CycleFinder(graph), hasCycle = true, cyclePath = listOf(E(2, 0), E(0, 1), E(1, 4), E(4, 2)))
    }

    @Test
    fun testCycleFound3() {
        val edges = arrayOf(
            E(0, 1),
            E(0, 2),
            E(1, 4),
            E(1, 3),
            E(2, 3),
            E(2, 5),
            E(3, 4),
            E(3, 5),
            E(4, 6), // 4 -> 6
            E(5, 4), // 5 -> 4
            E(6, 5), // 6 -> 5
        )
        val graph = createGraph(7, *edges)

        assertThat(CycleFinder(graph), hasCycle = true, cyclePath = listOf(E(5, 4), E(4, 6), E(6, 5)))
    }

    @Test
    fun testNoCycle() {
        val graph = createGraph(
            7,
            E(0, 1),
            E(0, 2),
            E(1, 4),
            E(1, 3),
            E(2, 3),
            E(2, 5),
            E(3, 4),
            E(3, 5),
            E(4, 6),
            E(5, 4),
            E(5, 6),
        )
        assertThat(CycleFinder(graph), hasCycle = false)
    }

    @Test
    fun testEmptyGraph() {
        assertThat(CycleFinder(createGraph(7)), hasCycle = false)
    }

    private fun assertThat(target: CycleFinder<E>, hasCycle: Boolean, cyclePath: List<Edge>? = null) {
        expect(hasCycle) { target.hasCycle() }
        expect(cyclePath) { target.cyclePath() }
    }

    private data class E(override val from: Int, override val to: Int) : Edge

    private fun createGraph(v: Int, vararg edges: E): Graph<E> {
        var graph = Graph<E>(v)
        edges.forEach { graph.addEdge(it) }
        return graph
    }
}
