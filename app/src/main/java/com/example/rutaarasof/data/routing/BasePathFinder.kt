package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphEdge
import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.RouteResult
import java.util.PriorityQueue

/**
 * Base path finder implementing Dijkstra's algorithm for shortest-path graph traversal.
 * Subclasses implement [edgeCost] to define how edge weights are calculated.
 */
abstract class BasePathFinder : RouteCalculator {

    protected abstract fun edgeCost(
        a: GraphNode,
        b: GraphNode,
        mapWidth: Float,
        mapHeight: Float
    ): Float

    override fun calculate(
        nodes: List<GraphNode>,
        edges: List<GraphEdge>,
        startNodeId: String,
        endNodeId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult? {
        if (nodes.isEmpty() || edges.isEmpty()) return null

        val nodeMap = nodes.associateBy { it.id }
        val startNode = nodeMap[startNodeId] ?: return null
        val endNode = nodeMap[endNodeId] ?: return null

        if (startNode.id == endNode.id) {
            return RouteResult(nodes = listOf(startNode), totalMeters = 0f)
        }

        // Build adjacency list for undirected graph
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        for (edge in edges) {
            val n1 = nodeMap[edge.from] ?: continue
            val n2 = nodeMap[edge.to] ?: continue

            val cost = edgeCost(n1, n2, mapWidth, mapHeight)

            adjacency.getOrPut(edge.from) { mutableListOf() }.add(Pair(edge.to, cost))
            adjacency.getOrPut(edge.to) { mutableListOf() }.add(Pair(edge.from, cost))
        }

        // Dijkstra's priority queue pathfinding
        val distances = mutableMapOf<String, Float>()
        val previous = mutableMapOf<String, String>()
        val priorityQueue = PriorityQueue<Pair<String, Float>>(compareBy { it.second })

        distances[startNode.id] = 0f
        priorityQueue.add(Pair(startNode.id, 0f))

        while (priorityQueue.isNotEmpty()) {
            val (u, currentDist) = priorityQueue.poll() ?: break

            if (u == endNode.id) break

            if (currentDist > (distances[u] ?: Float.POSITIVE_INFINITY)) continue

            val neighbors = adjacency[u] ?: continue
            for ((v, weight) in neighbors) {
                val newDist = currentDist + weight
                if (newDist < (distances[v] ?: Float.POSITIVE_INFINITY)) {
                    distances[v] = newDist
                    previous[v] = u
                    priorityQueue.add(Pair(v, newDist))
                }
            }
        }

        val totalCost = distances[endNode.id] ?: return null
        if (totalCost == Float.POSITIVE_INFINITY) return null

        // Reconstruct path from destination back to start
        val pathNodeIds = mutableListOf<String>()
        var current: String? = endNode.id
        while (current != null) {
            pathNodeIds.add(current)
            current = previous[current]
        }
        pathNodeIds.reverse()

        val pathNodes = pathNodeIds.mapNotNull { nodeMap[it] }
        if (pathNodes.isEmpty()) return null

        return RouteResult(nodes = pathNodes, totalMeters = totalCost)
    }
}
