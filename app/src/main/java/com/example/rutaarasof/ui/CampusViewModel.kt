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

// Extended ViewModel managing single-screen map navigation, search, and calibration mode
class CampusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CampusRepository(application.applicationContext)

    // StateFlow holding all campus map locations
    private val _locations = MutableStateFlow<List<CampusLocation>>(emptyList())
    val locations: StateFlow<List<CampusLocation>> = _locations.asStateFlow()

    // StateFlow holding all building rooms
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    // Search query StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Controls whether search dropdown list is visible
    private val _isDropdownExpanded = MutableStateFlow(false)
    val isDropdownExpanded: StateFlow<Boolean> = _isDropdownExpanded.asStateFlow()

    // Selected category filter ("All", "Building", "Gate", "Canteen", "Gym")
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Currently selected map location
    private val _selectedLocation = MutableStateFlow<CampusLocation?>(null)
    val selectedLocation: StateFlow<CampusLocation?> = _selectedLocation.asStateFlow()

    // Combined search results (locations + rooms)
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Calibrate mode switch toggle
    private val _isCalibrateMode = MutableStateFlow(false)
    val isCalibrateMode: StateFlow<Boolean> = _isCalibrateMode.asStateFlow()

    // Calibration target relative coordinates (x, y) when map is tapped in calibrate mode
    private val _calibrationTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val calibrationTarget: StateFlow<Pair<Double, Double>?> = _calibrationTarget.asStateFlow()

    init {
        _locations.value = repository.getAllLocations()
        _rooms.value = repository.rooms
        updateSearchResults()
    }

    // Called when search query changes
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        _isDropdownExpanded.value = newQuery.isNotBlank()
        updateSearchResults()
    }

    // Called when category filter chip is selected
    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        updateSearchResults()
    }

    // Show or hide dropdown list
    fun setDropdownExpanded(expanded: Boolean) {
        _isDropdownExpanded.value = expanded
    }

    // Select a location directly
    fun selectLocation(location: CampusLocation?) {
        _selectedLocation.value = location
    }

    // Select parent building when a room result is tapped
    fun selectBuildingByRoom(room: Room): CampusLocation? {
        val building = repository.getLocationById(room.buildingId)
        _selectedLocation.value = building
        return building
    }

    // Get rooms belonging to a specific building ID
    fun getRoomsByBuilding(buildingId: String): List<Room> {
        return repository.getRoomsByBuilding(buildingId)
    }

    // Toggle calibrate mode switch ON/OFF
    fun toggleCalibrateMode(enabled: Boolean) {
        _isCalibrateMode.value = enabled
        if (!enabled) {
            _calibrationTarget.value = null
        }
    }

    // Set calibration target coordinates (relative x, y)
    fun setCalibrationTarget(x: Double, y: Double) {
        _calibrationTarget.value = Pair(x, y)
    }

    // Clear calibration target
    fun clearCalibrationTarget() {
        _calibrationTarget.value = null
    }

    // Private helper to filter locations and rooms based on query and category
    private fun updateSearchResults() {
        val query = _searchQuery.value.trim()
        val category = _selectedCategory.value

        // Filter locations
        val matchingLocations = repository.searchLocations(query).filter { loc ->
            category == "All" || loc.category.equals(category, ignoreCase = true)
        }

        // Filter rooms (only shown if filter is "All" or "Building")
        val matchingRooms = if (category == "All" || category == "Building") {
            repository.searchRooms(query)
        } else {
            emptyList()
        }

        val locationResults = matchingLocations.map { SearchResult.LocationItem(it) }
        val roomResults = matchingRooms.map { room ->
            val buildingName = repository.getLocationById(room.buildingId)?.name ?: "Unknown Building"
            SearchResult.RoomItem(room, buildingName)
        }

        _searchResults.value = locationResults + roomResults
    }
}
