package com.example.rutaarasof.data.routing
import com.example.rutaarasof.data.model.GraphEdge
import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.RouteResult

interface RouteCalculator {
    fun calculate(
        nodes: List<GraphNode>,
        edges: List<GraphEdge>,
        startNodeId: String,
        endNodeId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult?
}
