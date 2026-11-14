package com.example.rutaarasof.data.model

import kotlinx.serialization.Serializable

// Data class representing a node in the walking-path graph
@Serializable
data class GraphNode(
    val id: String,
    val x: Double, // Relative X coordinate (0.0 to 1.0) on map image
    val y: Double, // Relative Y coordinate (0.0 to 1.0) on map image
    val locationId: String? = null // Optional ID of linked CampusLocation
)
