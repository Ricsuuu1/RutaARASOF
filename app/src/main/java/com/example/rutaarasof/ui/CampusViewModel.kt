package com.example.rutaarasof.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.rutaarasof.data.model.CampusLocation
import com.example.rutaarasof.data.model.GraphData
import com.example.rutaarasof.data.model.GraphEdge
import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.METERS_PER_PIXEL
import com.example.rutaarasof.data.model.Room
import com.example.rutaarasof.data.model.RouteResult
import com.example.rutaarasof.data.model.SearchResult
import com.example.rutaarasof.data.repository.CampusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import java.util.PriorityQueue
import kotlin.math.round
import kotlin.math.sqrt

// Maximum allowed distance in image pixels from tap to nearest node for setting start point
const val MAX_PICK_START_DISTANCE_PX = 250f

// Extended ViewModel managing map navigation, search, calibration mode, graph edit mode, and walking routing
class CampusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CampusRepository(application.applicationContext)
    private val jsonParser = Json { ignoreUnknownKeys = true; prettyPrint = true }

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

    // Selected category filter ("All", "Building", "Gate", "Canteen", "Gym", "Facility")
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

    // --- GRAPH EDIT MODE STATEFLOWS ---
    private val _isGraphMode = MutableStateFlow(false)
    val isGraphMode: StateFlow<Boolean> = _isGraphMode.asStateFlow()

    private val _nodes = MutableStateFlow<List<GraphNode>>(emptyList())
    val nodes: StateFlow<List<GraphNode>> = _nodes.asStateFlow()

    private val _edges = MutableStateFlow<List<GraphEdge>>(emptyList())
    val edges: StateFlow<List<GraphEdge>> = _edges.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    // --- ROUTING & PICK START STATEFLOWS ---
    private val _startNodeId = MutableStateFlow<String?>(null)
    val startNodeId: StateFlow<String?> = _startNodeId.asStateFlow()

    private val _startLabel = MutableStateFlow<String?>(null)
    val startLabel: StateFlow<String?> = _startLabel.asStateFlow()

    private val _routeDestinationId = MutableStateFlow<String?>(null)
    val routeDestinationId: StateFlow<String?> = _routeDestinationId.asStateFlow()

    private val _isPickStartMode = MutableStateFlow(false)
    val isPickStartMode: StateFlow<Boolean> = _isPickStartMode.asStateFlow()

    private val _activeRoute = MutableStateFlow<RouteResult?>(null)
    val activeRoute: StateFlow<RouteResult?> = _activeRoute.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()

    // Undo history stack
    private val history = ArrayDeque<GraphData>()

    // File reference for saved graph in internal storage
    private val savedGraphFile: File by lazy {
        File(getApplication<Application>().filesDir, "graph_saved.json")
    }

    init {
        _locations.value = repository.getAllLocations()
        _rooms.value = repository.rooms
        updateSearchResults()
        loadGraphData()
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

    // Toggle calibrate mode switch ON/OFF (mutually exclusive with Graph Mode & Pick Start Mode)
    fun toggleCalibrateMode(enabled: Boolean) {
        _isCalibrateMode.value = enabled
        if (enabled) {
            _isGraphMode.value = false
            _isPickStartMode.value = false
            _selectedNodeId.value = null
        } else {
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

    // --- GRAPH EDIT MODE METHODS ---

    // Toggle Graph Edit Mode switch ON/OFF (mutually exclusive with Calibrate Mode & Pick Start Mode)
    fun toggleGraphMode(enabled: Boolean) {
        _isGraphMode.value = enabled
        if (enabled) {
            _isCalibrateMode.value = false
            _isPickStartMode.value = false
            _calibrationTarget.value = null
        } else {
            _selectedNodeId.value = null
        }
    }

    // Select or deselect a graph node directly
    fun selectNode(nodeId: String?) {
        _selectedNodeId.value = nodeId
    }

    // Handle tapping a graph node (select, deselect, or chain edge creation)
    fun handleNodeTap(tappedNodeId: String) {
        val currentSelected = _selectedNodeId.value
        when {
            currentSelected == null -> {
                _selectedNodeId.value = tappedNodeId
            }
            currentSelected == tappedNodeId -> {
                _selectedNodeId.value = null // Deselect when tapped again
            }
            else -> {
                // Toggle edge between currently selected node and tapped node, then select tapped node for chaining
                toggleEdge(currentSelected, tappedNodeId)
                _selectedNodeId.value = tappedNodeId
            }
        }
    }

    // Add a new node at relative map coordinates (x, y). If a node was previously selected, connect an edge from it to the new node.
    fun addNode(x: Double, y: Double) {
        pushHistoryState()

        val previousSelectedId = _selectedNodeId.value

        // Generate next auto-increment ID (e.g. n1, n2, n3...)
        val existingIndices = _nodes.value.mapNotNull { node ->
            if (node.id.startsWith("n")) node.id.substring(1).toIntOrNull() else null
        }
        val nextNumber = (existingIndices.maxOrNull() ?: 0) + 1
        val newNodeId = "n$nextNumber"

        val roundedX = round(x * 1000.0) / 1000.0
        val roundedY = round(y * 1000.0) / 1000.0

        val newNode = GraphNode(id = newNodeId, x = roundedX, y = roundedY)
        _nodes.value = _nodes.value + newNode

        // If a node was previously selected, automatically connect an edge to the new node (1 undo step)
        if (previousSelectedId != null) {
            _edges.value = _edges.value + GraphEdge(from = previousSelectedId, to = newNodeId)
        }

        _selectedNodeId.value = newNodeId

        saveGraphData()
    }

    // Toggle an edge between fromId and toId (adds if missing, removes if present)
    fun toggleEdge(fromId: String, toId: String) {
        if (fromId == toId) return
        pushHistoryState()

        val existingEdge = _edges.value.find {
            (it.from == fromId && it.to == toId) || (it.from == toId && it.to == fromId)
        }

        _edges.value = if (existingEdge != null) {
            _edges.value - existingEdge
        } else {
            _edges.value + GraphEdge(from = fromId, to = toId)
        }

        saveGraphData()
    }

    // Delete currently selected node and all connected edges
    fun deleteSelectedNode() {
        val nodeId = _selectedNodeId.value ?: return
        pushHistoryState()

        _nodes.value = _nodes.value.filterNot { it.id == nodeId }
        _edges.value = _edges.value.filterNot { it.from == nodeId || it.to == nodeId }
        _selectedNodeId.value = null

        saveGraphData()
    }

    // Link currently selected node to a CampusLocation ID (enforces 1-to-1 link)
    fun linkSelectedNodeToLocation(locationId: String?) {
        val nodeId = _selectedNodeId.value ?: return
        pushHistoryState()

        _nodes.value = _nodes.value.map { node ->
            when {
                node.id == nodeId -> node.copy(locationId = locationId)
                locationId != null && node.locationId == locationId -> node.copy(locationId = null) // Clear previous link
                else -> node
            }
        }

        saveGraphData()
    }

    // Revert to previous graph state from history stack
    fun undoGraphAction() {
        if (history.isNotEmpty()) {
            val prevState = history.removeLast()
            _nodes.value = prevState.nodes
            _edges.value = prevState.edges
            _selectedNodeId.value = null
            saveGraphData()
        }
    }

    // Generate formatted Graph JSON string for clipboard export
    fun generateGraphJson(): String {
        val nodesJsonList = _nodes.value.map { node ->
            val locPart = if (node.locationId != null) ",\"locationId\":\"${node.locationId}\"" else ""
            "{\"id\":\"${node.id}\",\"x\":${String.format(Locale.US, "%.3f", node.x)},\"y\":${String.format(Locale.US, "%.3f", node.y)}$locPart}"
        }
        val edgesJsonList = _edges.value.map { edge ->
            "{\"from\":\"${edge.from}\",\"to\":\"${edge.to}\"}"
        }

        val nodesStr = nodesJsonList.joinToString(",")
        val edgesStr = edgesJsonList.joinToString(",")

        return "{\"nodes\":[$nodesStr],\"edges\":[$edgesStr]}"
    }

    // --- WALKING ROUTING & PICK START METHODS ---

    // Toggle Pick Start Mode ON/OFF
    fun togglePickStartMode(enabled: Boolean) {
        _isPickStartMode.value = enabled
        if (enabled) {
            _isCalibrateMode.value = false
            _isGraphMode.value = false
        }
    }

    // Set a CampusLocation as the starting point (finds node linked to location)
    fun setStartLocation(location: CampusLocation): Boolean {
        val linkedNode = _nodes.value.find { it.locationId.equals(location.id, ignoreCase = true) }
        if (linkedNode != null) {
            _startNodeId.value = linkedNode.id
            _startLabel.value = location.name
            clearRoute()
            return true
        } else {
            clearRoute()
            _routeError.value = "Route not available"
            return false
        }
    }

    // Directly set start node ID and label string
    fun setStartNode(nodeId: String, label: String) {
        _startNodeId.value = nodeId
        _startLabel.value = label
        clearRoute()
    }

    // Clear starting location/node
    fun clearStartLocation() {
        _startNodeId.value = null
        _startLabel.value = null
        clearRoute()
    }

    // Clear active route polyline and destination
    fun clearRoute() {
        _activeRoute.value = null
        _routeDestinationId.value = null
        _routeError.value = null
    }

    // Pick nearest graph node to relative (relX, relY) map tap (must be within MAX_PICK_START_DISTANCE_PX image pixels)
    fun pickNearestStartNode(relX: Double, relY: Double, imageWidth: Float, imageHeight: Float): Boolean {
        if (_nodes.value.isEmpty()) return false

        var minDistanceSq = Float.MAX_VALUE
        var nearestNode: GraphNode? = null

        for (node in _nodes.value) {
            val dx = (node.x - relX).toFloat() * imageWidth
            val dy = (node.y - relY).toFloat() * imageHeight
            val distSq = dx * dx + dy * dy
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq
                nearestNode = node
            }
        }

        val maxDistSq = MAX_PICK_START_DISTANCE_PX * MAX_PICK_START_DISTANCE_PX
        if (nearestNode == null || minDistanceSq > maxDistSq) {
            // Nearest node is farther than 250 image pixels: do not set start, stay in pick mode, return false
            return false
        }

        val label = if (nearestNode.locationId != null) {
            val loc = repository.getLocationById(nearestNode.locationId)
            loc?.name ?: "Custom point"
        } else {
            "Custom point"
        }

        setStartNode(nearestNode.id, label)
        _isPickStartMode.value = false
        return true
    }

    // Calculate walking route from startNodeId to destinationLocation using Dijkstra algorithm
    fun calculateRoute(destinationLocation: CampusLocation, mapWidth: Float, mapHeight: Float) {
        val sNodeId = _startNodeId.value
        if (sNodeId == null) {
            _activeRoute.value = null
            _routeDestinationId.value = null
            _routeError.value = "Set a start location first"
            return
        }

        // Find graph node linked to destination location
        val endNode = _nodes.value.find { it.locationId.equals(destinationLocation.id, ignoreCase = true) }

        if (endNode != null && sNodeId == endNode.id) {
            // Start node is the same as destination's node
            _activeRoute.value = null
            _routeDestinationId.value = null
            _routeError.value = "You're already here"
            return
        }

        val result = findRouteFromNode(sNodeId, destinationLocation.id, mapWidth, mapHeight)
        if (result != null) {
            _activeRoute.value = result
            _routeDestinationId.value = destinationLocation.id
            _routeError.value = null
        } else {
            _activeRoute.value = null
            _routeDestinationId.value = null
            _routeError.value = "Route not available"
        }
    }

    // Core Dijkstra pathfinder between startNodeId and endNodeId
    private fun findRouteBetweenNodes(
        startNodeId: String,
        endNodeId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult? {
        val currentNodes = _nodes.value
        val currentEdges = _edges.value

        if (currentNodes.isEmpty() || currentEdges.isEmpty()) return null

        val nodeMap = currentNodes.associateBy { it.id }
        val startNode = nodeMap[startNodeId] ?: return null
        val endNode = nodeMap[endNodeId] ?: return null

        if (startNode.id == endNode.id) {
            return RouteResult(nodes = listOf(startNode), totalMeters = 0f)
        }

        // Build adjacency list for undirected graph: nodeId -> List<Pair<neighborId, distanceMeters>>
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        for (edge in currentEdges) {
            val n1 = nodeMap[edge.from] ?: continue
            val n2 = nodeMap[edge.to] ?: continue

            val dx = (n2.x - n1.x).toFloat() * mapWidth
            val dy = (n2.y - n1.y).toFloat() * mapHeight
            val distPx = sqrt(dx * dx + dy * dy)
            val distMeters = distPx * METERS_PER_PIXEL

            adjacency.getOrPut(edge.from) { mutableListOf() }.add(Pair(edge.to, distMeters))
            adjacency.getOrPut(edge.to) { mutableListOf() }.add(Pair(edge.from, distMeters))
        }

        // Dijkstra PriorityQueue
        val distances = mutableMapOf<String, Float>()
        val previous = mutableMapOf<String, String>()
        val priorityQueue = PriorityQueue<Pair<String, Float>>(compareBy { it.second })

        distances[startNode.id] = 0f
        priorityQueue.add(Pair(startNode.id, 0f))

        while (priorityQueue.isNotEmpty()) {
            val (u, currentDist) = priorityQueue.poll() ?: break

            if (u == endNode.id) break // Destination reached

            if (currentDist > (distances[u] ?: Float.POSITIVE_INFINITY)) continue

            val neighbors = adjacency[u] ?: continue
            for ((v, weight) in neighbors) {
                val newDist = currentDist + weight
                if (newDist < (distances[v] ?: Float.POSITIVE_INFINITY)) {
                    distances[v] = newDist
                    previous[v] = u
                    priorityQueue.add(Pair(v, newDist))
                }
            }
        }

        val totalDist = distances[endNode.id] ?: return null
        if (totalDist == Float.POSITIVE_INFINITY) return null

        // Reconstruct path from destination back to start
        val pathNodeIds = mutableListOf<String>()
        var current: String? = endNode.id
        while (current != null) {
            pathNodeIds.add(current)
            current = previous[current]
        }
        pathNodeIds.reverse()

        val pathNodes = pathNodeIds.mapNotNull { nodeMap[it] }
        if (pathNodes.isEmpty()) return null

        return RouteResult(nodes = pathNodes, totalMeters = totalDist)
    }

    // Find route from startNodeId to endLocationId
    fun findRouteFromNode(
        startNodeId: String,
        endLocationId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult? {
        val endNode = _nodes.value.find { it.locationId.equals(endLocationId, ignoreCase = true) }
            ?: return null
        return findRouteBetweenNodes(startNodeId, endNode.id, mapWidth, mapHeight)
    }

    // Find route from startLocationId to endLocationId (reuses findRouteBetweenNodes)
    fun findRoute(
        startLocationId: String,
        endLocationId: String,
        mapWidth: Float,
        mapHeight: Float
    ): RouteResult? {
        val startNode = _nodes.value.find { it.locationId.equals(startLocationId, ignoreCase = true) }
            ?: return null
        return findRouteFromNode(startNode.id, endLocationId, mapWidth, mapHeight)
    }

    // Push current graph state to undo history before making changes
    private fun pushHistoryState() {
        history.addLast(GraphData(nodes = _nodes.value, edges = _edges.value))
        if (history.size > 50) {
            history.removeFirst()
        }
    }

    // Save current graph to internal storage
    private fun saveGraphData() {
        try {
            val graphData = GraphData(nodes = _nodes.value, edges = _edges.value)
            val jsonString = jsonParser.encodeToString(GraphData.serializer(), graphData)
            savedGraphFile.writeText(jsonString)
        } catch (e: Exception) {
            Log.e("CampusViewModel", "Failed to save graph data to internal storage", e)
        }
    }

    // Load graph data from internal storage, fallback to assets/graph.json, or start empty
    private fun loadGraphData() {
        try {
            if (savedGraphFile.exists()) {
                val jsonString = savedGraphFile.readText()
                val graphData = jsonParser.decodeFromString(GraphData.serializer(), jsonString)
                _nodes.value = graphData.nodes
                _edges.value = graphData.edges
                return
            }

            // Fallback: check assets/graph.json
            val context = getApplication<Application>().applicationContext
            val assetNames = context.assets.list("") ?: emptyArray()
            if (assetNames.contains("graph.json")) {
                val jsonString = context.assets.open("graph.json").bufferedReader().use { it.readText() }
                val graphData = jsonParser.decodeFromString(GraphData.serializer(), jsonString)
                _nodes.value = graphData.nodes
                _edges.value = graphData.edges
                return
            }
        } catch (e: Exception) {
            Log.e("CampusViewModel", "Failed to load graph data, starting empty", e)
        }

        _nodes.value = emptyList()
        _edges.value = emptyList()
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
