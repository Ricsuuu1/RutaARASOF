package com.example.rutaarasof.data.model

// Sealed class representing search result items: either a Location or a Room
sealed class SearchResult {
    data class LocationItem(val location: CampusLocation) : SearchResult()
    data class RoomItem(val room: Room, val buildingName: String) : SearchResult()
}
