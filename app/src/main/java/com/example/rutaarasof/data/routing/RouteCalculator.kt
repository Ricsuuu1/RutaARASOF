package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphEdge
import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.RouteResult

/**
 * Strategy interface for calculating optimal routes between nodes in a graph network.
 */
interface RouteCalculator {
    /**
     * Calculates an optimal route from [startNodeId] to [endNodeId].
     *
     * @return [RouteResult] containing ordered path nodes and total route cost, or null if unlinked/unreachable.
     */
    fun calculate(
        nodes: List<GraphNode>,
        edges: List<GraphEdge>,
        startNodeId: String,
        endNodeId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult?
}
