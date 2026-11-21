package com.example.rutaarasof.data.repository

import android.content.Context
import com.example.rutaarasof.data.model.CampusLocation
import com.example.rutaarasof.data.model.Room
import kotlinx.serialization.json.Json

class CampusRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val locations: List<CampusLocation> by lazy {
        loadJsonFromAsset("locations.json")
    }

    val rooms: List<Room> by lazy {
        loadJsonFromAsset("rooms.json")
    }

    private inline fun <reified T> loadJsonFromAsset(fileName: String): T {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return json.decodeFromString(jsonString)
    }

    fun getAllLocations(): List<CampusLocation> {
        return locations
    }

    fun getLocationById(id: String): CampusLocation? {
        return locations.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getRoomsByBuilding(buildingId: String): List<Room> {
        return rooms.filter { it.buildingId.equals(buildingId, ignoreCase = true) }
    }

    fun searchLocations(query: String): List<CampusLocation> {
        if (query.isBlank()) return locations
        val q = query.trim().lowercase()
        return locations.filter {
            it.name.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }
    }

    fun searchRooms(query: String): List<Room> {
        if (query.isBlank()) return rooms
        val q = query.trim().lowercase()
        return rooms.filter {
            it.name.lowercase().contains(q) ||
            it.type.lowercase().contains(q)
        }
    }
}
