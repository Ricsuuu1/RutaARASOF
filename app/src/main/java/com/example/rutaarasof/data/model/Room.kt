package com.example.rutaarasof.data.model

import kotlinx.serialization.Serializable

// Data class representing a room inside a campus building
@Serializable
data class Room(
    val id: String,
    val name: String,
    val buildingId: String, // ID of the parent building
    val floor: Int,
    val type: String // "Classroom", "Lab", "Office", "Restroom", "Other"
)
