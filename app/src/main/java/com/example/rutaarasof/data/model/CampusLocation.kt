package com.example.rutaarasof.data.model

import kotlinx.serialization.Serializable

// Data class representing a campus location on the map (Building, Gate, Canteen, Gym)
@Serializable
data class CampusLocation(
    val id: String,
    val name: String,
    val category: String, // "Building", "Gate", "Canteen", "Gym"
    val description: String,
    val x: Double, // Relative X position on map image (0.0 to 1.0)
    val y: Double  // Relative Y position on map image (0.0 to 1.0)
)
