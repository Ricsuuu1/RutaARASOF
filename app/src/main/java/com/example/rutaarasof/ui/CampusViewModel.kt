package com.example.rutaarasof.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.rutaarasof.data.model.CampusLocation
import com.example.rutaarasof.data.model.Room
import com.example.rutaarasof.data.model.SearchResult
import com.example.rutaarasof.data.repository.CampusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel managing campus state, search queries, and selected locations
class CampusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CampusRepository(application.applicationContext)

    // StateFlow holding all campus locations
    private val _locations = MutableStateFlow<List<CampusLocation>>(emptyList())
    val locations: StateFlow<List<CampusLocation>> = _locations.asStateFlow()

    // StateFlow holding all campus rooms
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    // Search query StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected category filter StateFlow ("All", "Building", "Gate", "Canteen", "Gym")
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Currently selected location on map/details
    private val _selectedLocation = MutableStateFlow<CampusLocation?>(null)
    val selectedLocation: StateFlow<CampusLocation?> = _selectedLocation.asStateFlow()

    // Search results StateFlow combining locations and rooms
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    init {
        // Load locations and rooms from repository on startup
        _locations.value = repository.getAllLocations()
        _rooms.value = repository.rooms
        updateSearchResults()
    }

    // Called when user types in the search bar
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        updateSearchResults()
    }

    // Called when user selects a category filter chip
    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        updateSearchResults()
    }

    // Select a location directly
    fun selectLocation(location: CampusLocation?) {
        _selectedLocation.value = location
    }

    // Select a building by room, finding the parent building from the repository
    fun selectBuildingByRoom(room: Room): CampusLocation? {
        val building = repository.getLocationById(room.buildingId)
        _selectedLocation.value = building
        return building
    }

    // Filter locations and rooms based on current search query and category filter
    private fun updateSearchResults() {
        val query = _searchQuery.value.trim()
        val category = _selectedCategory.value

        // Filter locations matching query & category
        val matchingLocations = repository.searchLocations(query).filter { loc ->
            category == "All" || loc.category.equals(category, ignoreCase = true)
        }

        // Filter rooms (shown only when category is "All" or "Building")
        val matchingRooms = if (category == "All" || category == "Building") {
            repository.searchRooms(query)
        } else {
            emptyList()
        }

        // Map matching items into SearchResult wrapper objects
        val locationResults = matchingLocations.map { SearchResult.LocationItem(it) }
        val roomResults = matchingRooms.map { room ->
            val buildingName = repository.getLocationById(room.buildingId)?.name ?: "Unknown Building"
            SearchResult.RoomItem(room, buildingName)
        }

        _searchResults.value = locationResults + roomResults
    }
}
