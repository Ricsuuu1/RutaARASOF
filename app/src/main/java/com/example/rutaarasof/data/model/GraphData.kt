package com.example.rutaarasof.data.model

import kotlinx.serialization.Serializable

// Wrapper container data class for serializing and deserializing graph JSON
@Serializable
data class GraphData(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)
