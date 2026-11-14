package com.example.rutaarasof.data.model

import kotlinx.serialization.Serializable

// Data class representing an undirected edge connecting two graph nodes
@Serializable
data class GraphEdge(
    val from: String, // ID of source node
    val to: String    // ID of target node
)
