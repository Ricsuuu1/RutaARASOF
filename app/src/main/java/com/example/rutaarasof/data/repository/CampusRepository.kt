package com.example.rutaarasof.data.repository

import android.content.Context
import com.example.rutaarasof.data.model.CampusLocation
import com.example.rutaarasof.data.model.Room
import kotlinx.serialization.json.Json

// Repository to load and search campus data from local JSON assets
class CampusRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // Lazy list of all campus map locations loaded from assets/locations.json
    val locations: List<CampusLocation> by lazy {
        loadJsonFromAsset("locations.json")
    }

    // Lazy list of all building rooms loaded from assets/rooms.json
    val rooms: List<Room> by lazy {
        loadJsonFromAsset("rooms.json")
    }

    // Helper method to read and decode JSON files from assets
    private inline fun <reified T> loadJsonFromAsset(fileName: String): T {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return json.decodeFromString(jsonString)
    }

    // Get all map locations
    fun getAllLocations(): List<CampusLocation> {
        return locations
    }

    // Get a single location by its unique ID
    fun getLocationById(id: String): CampusLocation? {
        return locations.find { it.id.equals(id, ignoreCase = true) }
    }

    // Get all rooms belonging to a specific building ID
    fun getRoomsByBuilding(buildingId: String): List<Room> {
        return rooms.filter { it.buildingId.equals(buildingId, ignoreCase = true) }
    }

    // Case-insensitive search for locations by name, category, or description
    fun searchLocations(query: String): List<CampusLocation> {
        if (query.isBlank()) return locations
        val q = query.trim().lowercase()
        return locations.filter {
            it.name.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }
    }

    // Case-insensitive search for rooms by name or type
    fun searchRooms(query: String): List<Room> {
        if (query.isBlank()) return rooms
        val q = query.trim().lowercase()
        return rooms.filter {
            it.name.lowercase().contains(q) ||
            it.type.lowercase().contains(q)
        }
    }
}
