import com.github.papahigh.BellmanFordWeight
import com.github.papahigh.CycleFinder
import com.github.papahigh.Edge
import com.github.papahigh.Graph
import kotlin.test.Test
import kotlin.test.expect


class BellmanFordTest {

    @Test
    fun testEnterPathToNegativeCycle() {
        val edges = arrayOf(
            Edge(0, 1, .9),
            Edge(0, 2, .9),
            Edge(1, 3, .8),
            Edge(1, 4, .6),
            Edge(2, 3, .67),
            Edge(2, 5, .49),
            Edge(3, 4, .8),
            Edge(3, 5, .74), // 3 -> 5
            Edge(4, 3, .8), // 4 -> 3
            Edge(4, 6, .67),
            Edge(5, 4, .9),
            Edge(5, 6, 2.2), // 5 -> 6
            Edge(6, 4, .9), // 6 -> 4
        )
        val graph = graph(7, *edges)

        assert(
            BellmanFordWeight(graph, 0),
            enterPath = listOf(edges[1], edges[5]), // 0 -> 2 -> 5
            cyclePath = listOf(edges[11], edges[12], edges[8], edges[7]) // 5 -> 6 -> 4 -> 3 -> 5
        )

        assert(
            BellmanFordWeight(graph, 1),
            enterPath = listOf(edges[3]), // 1 -> 4
            cyclePath = listOf(edges[8], edges[7], edges[11], edges[12]) // 4 -> 3 -> 5 -> 6 -> 4
        )

        assert(
            BellmanFordWeight(graph, 6),
            enterPath = emptyList(),
            cyclePath = listOf(edges[12], edges[8], edges[7], edges[11]) // 6 -> 4 -> 3 -> 5 -> 6
        )
    }

    @Test
    fun testNoShortestPath() {
        val edges = arrayOf(
            Edge(0, 1, .9), Edge(0, 2, .9),
            Edge(1, 3, .9), Edge(1, 4, .6),
            Edge(2, 3, .7), Edge(2, 5, .49),
            Edge(3, 4, .4), Edge(3, 5, .7),
            Edge(4, 5, .9), Edge(4, 6, .67),
            Edge(5, 4, .9), Edge(5, 6, .9),
        )
        val graph = graph(7, *edges)
        var target = BellmanFordWeight(graph, 3)

        expect(emptyList()) { target.shortestPathTo(0) } // unreachable
        expect(emptyList()) { target.shortestPathTo(3) } // already there
        expect(listOf(edges[7], edges[11])) { target.shortestPathTo(6) } // 3 -> 5 -> 6

        assert(target, enterPath = null, cyclePath = null)
    }

    private fun assert(target: BellmanFordWeight, enterPath: List<Edge>?, cyclePath: List<Edge>?) {
        expect(enterPath) { target.negativeEnterPath() }
        expect(cyclePath) { target.negativeCyclePath() }
        expect(cyclePath != null) { target.hasNegativeCycle() }
    }
}

class CycleFinderTest {

    @Test
    fun testCycleFound1() {
        val edges = arrayOf(
            Edge(0, 1, .1),
            Edge(0, 2, .1),
            Edge(1, 2, .1), // 1 -> 2
            Edge(1, 4, .1),
            Edge(2, 5, .1), // 2 -> 5
            Edge(3, 1, .1), // 3 -> 1
            Edge(3, 4, .1),
            Edge(4, 6, .1),
            Edge(5, 3, .1), // 5 -> 3
            Edge(5, 4, .1),
            Edge(5, 6, .1),
        )
        val graph = graph(7, *edges)

        assert(
            CycleFinder(graph, 0),
            hasCycle = true,
            enterPath = listOf(edges[0]), // 0 -> 1
            cyclePath = listOf(edges[2], edges[4], edges[8], edges[5]) // 1 -> 2 -> 5 -> 3 -> 1
        )

        assert(
            CycleFinder(graph, 3),
            hasCycle = true,
            enterPath = emptyList(),
            cyclePath = listOf(edges[5], edges[2], edges[4], edges[8]) // 3 -> 1 -> 2 -> 5 -> 3
        )
    }

    @Test
    fun testCycleFound2() {
        val edges = arrayOf(
            Edge(0, 1, .1), // 0 -> 1
            Edge(0, 2, .1),
            Edge(1, 4, .1), // 1 -> 4
            Edge(1, 3, .1),
            Edge(2, 0, .1), // 2 -> 0
            Edge(2, 3, .1),
            Edge(2, 5, .1),
            Edge(3, 4, .1),
            Edge(3, 5, .1),
            Edge(4, 2, .1), // 4 -> 2
            Edge(4, 6, .1),
            Edge(5, 4, .1),
            Edge(5, 6, .1),
        )
        val graph = graph(7, *edges)

        assert(
            CycleFinder(graph, 0),
            hasCycle = true,
            enterPath = emptyList(),
            cyclePath = listOf(edges[0], edges[2], edges[9], edges[4]) // 0 -> 1 -> 4 -> 2 -> 0
        )
    }

    @Test
    fun testCycleFound3() {
        val edges = arrayOf(
            Edge(0, 1, .1),
            Edge(0, 2, .1),
            Edge(1, 4, .1),
            Edge(1, 3, .1),
            Edge(2, 3, .1),
            Edge(2, 5, .1), // enter
            Edge(3, 4, .1),
            Edge(3, 5, .1),
            Edge(4, 6, .1), // 4 -> 6
            Edge(5, 4, .1), // 5 -> 4
            Edge(6, 5, .1), // 6 -> 5
        )
        val graph = graph(7, *edges)

        assert(
            CycleFinder(graph, 2),
            hasCycle = true,
            enterPath = listOf(edges[5]), // 2 -> 5
            cyclePath = listOf(edges[9], edges[8], edges[10]) // 5 -> 4 -> 6 -> 5
        )
    }

    @Test
    fun testNoCycle() {
        val graph = graph(
            7,
            Edge(0, 1, .1),
            Edge(0, 2, .1),
            Edge(1, 4, .1),
            Edge(1, 3, .1),
            Edge(2, 3, .1),
            Edge(2, 5, .1),
            Edge(3, 4, .1),
            Edge(3, 5, .1),
            Edge(4, 6, .1),
            Edge(5, 4, .1),
            Edge(5, 6, .1),
        )
        assert(CycleFinder(graph, 0), hasCycle = false, enterPath = null, cyclePath = null)
    }

    @Test
    fun testEmptyGraph() {
        assert(CycleFinder(graph(7), 0), hasCycle = false, enterPath = null, cyclePath = null)
    }

    private fun assert(target: CycleFinder, hasCycle: Boolean, enterPath: List<Edge>?, cyclePath: List<Edge>?) {
        expect(hasCycle) { target.hasCycle() }
        expect(enterPath) { target.enterPath() }
        expect(cyclePath) { target.cyclePath() }
    }
}

fun graph(vertices: Int, vararg edges: Edge): Graph {
    var graph = Graph(vertices)
    edges.forEach { graph.addEdge(it) }
    return graph
}
