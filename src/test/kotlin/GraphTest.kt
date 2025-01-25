import com.github.papahigh.*
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.expect


class SymbolGraphTest {

    private val sg = SymbolGraph.of<String, E>(listOf("A", "B", "C", "D", "E", "F"))

    @Test
    fun testSymbolGraph() {
        addEdges(
            edge("A", "B"),
            edge("A", "C"),
            edge("B", "D"),
            edge("C", "E"),
            edge("C", "F"),
            edge("F", "E"),
        )

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

    private fun addEdges(vararg edges: E) {
        for (edge in edges)
            sg.addEdge(edge)
    }

    private fun edge(from: String, to: String): E {
        return E(sg.indexOf(from)!!, sg.indexOf(to)!!)
    }

    private data class E(override val from: Int, override val to: Int) : Edge
}

class BellmanFordTest {

    private val sg = SymbolGraph.of<String, E>(listOf("USD", "EUR", "GBP", "CHF", "CAD"))

    @Test
    fun testArbitrageFound() {

        addEdges(
            edge("USD", "EUR", 0.741),
            edge("USD", "GBP", 0.657),
            edge("USD", "CHF", 1.061),
            edge("USD", "CAD", 1.005),
            edge("EUR", "USD", 1.349),
            edge("EUR", "GBP", 0.888),
            edge("EUR", "CHF", 1.433),
            edge("EUR", "CAD", 1.366),
            edge("GBP", "USD", 1.521),
            edge("GBP", "EUR", 1.126),
            edge("GBP", "CHF", 1.614),
            edge("GBP", "CAD", 1.538),
            edge("CHF", "USD", 0.942),
            edge("CHF", "EUR", 0.698),
            edge("CHF", "GBP", 0.619),
            edge("CHF", "CAD", 0.953),
            edge("CAD", "USD", 0.995),
            edge("CAD", "EUR", 0.732),
            edge("CAD", "GBP", 0.650),
            edge("CAD", "CHF", 0.049),
        )

        var target = BellmanFord.of("USD", sg)

        assertThat(
            target,
            hasCycle = true,
            cyclePath = listOf(edge("GBP", "USD", 1.521), edge("USD", "EUR", 0.741), edge("EUR", "GBP", 0.888))
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

    private fun edge(from: String, to: String, price: Double): E {
        return E(sg.indexOf(from)!!, sg.indexOf(to)!!, -ln(price))
    }

    data class E(override val from: Int, override val to: Int, override val weight: Double) : WeightedEdge
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
            E(4, 6), // 4 -> 6
            E(5, 4), // 5 -> 4
            E(6, 5), // 6 -> 5
        )

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

    private fun assertThat(target: CycleFinder<E>, hasCycle: Boolean, cyclePath: List<E>? = null) {
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
