package com.example.rutaarasof.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rutaarasof.R
import com.example.rutaarasof.data.model.CampusLocation
import com.example.rutaarasof.data.model.DEV_TOOLS_ENABLED
import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.SearchResult
import com.example.rutaarasof.ui.CampusViewModel
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Helper function to clamp pan translations so map boundaries never leave the visible screen area
private fun clampPan(
    scale: Float,
    panX: Float,
    panY: Float,
    drawWidth: Float,
    drawHeight: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    topOverlayPx: Float,
    bottomOverlayPx: Float
): Pair<Float, Float> {
    val centerX = containerWidthPx / 2f
    val centerY = containerHeightPx / 2f

    val scaledHalfWidth = (drawWidth * scale) / 2f
    val scaledHalfHeight = (drawHeight * scale) / 2f

    // Horizontal bounds
    val maxPanX = (scaledHalfWidth - centerX).coerceAtLeast(0f)
    val clampedX = panX.coerceIn(-maxPanX, maxPanX)

    // Vertical bounds with extra pan room for top search bar and bottom location card
    val maxPanYDown = (scaledHalfHeight - centerY + topOverlayPx).coerceAtLeast(0f)
    val maxPanYUp = (scaledHalfHeight - centerY + bottomOverlayPx).coerceAtLeast(0f)

    val clampedY = panY.coerceIn(-maxPanYUp, maxPanYDown)

    return Pair(clampedX, clampedY)
}

// Single-Screen Map View with pinch-to-zoom, pan, search, location card, calibration, graph editing, and walking routing
@Composable
fun MapScreen(viewModel: CampusViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Collect states from ViewModel
    val locations by viewModel.locations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDropdownExpanded by viewModel.isDropdownExpanded.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Mode toggles
    val isCalibrateMode by viewModel.isCalibrateMode.collectAsState()
    val calibrationTarget by viewModel.calibrationTarget.collectAsState()

    val isGraphMode by viewModel.isGraphMode.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()

    // Routing and Pick Start states
    val startNodeId by viewModel.startNodeId.collectAsState()
    val startLabel by viewModel.startLabel.collectAsState()
    val routeDestinationId by viewModel.routeDestinationId.collectAsState()
    val isPickStartMode by viewModel.isPickStartMode.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val routeError by viewModel.routeError.collectAsState()

    // Measure heights for top and bottom overlays
    val topOverlayPx = with(density) { 180.dp.toPx() }
    val bottomOverlayPx = if (selectedLocation != null) with(density) { 220.dp.toPx() } else 0f

    // Map Zoom and Pan states (panY starts offset to place map below top search bar)
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember(topOverlayPx) { mutableFloatStateOf(topOverlayPx / 2f) }

    // State for expanding/collapsing room list in bottom location card
    var isRoomsExpanded by remember { mutableStateOf(false) }

    // Categories filter list
    val categories = listOf("All", "Building", "Gate", "Canteen", "Gym", "Facility")

    // Load campus map image bitmap once
    val mapBitmap = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.campus_map).asImageBitmap()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val imageWidth = mapBitmap.width.toFloat()
        val imageHeight = mapBitmap.height.toFloat()
        val imageAspect = imageWidth / imageHeight
        val containerAspect = containerWidthPx / containerHeightPx

        // Calculate aspect-ratio fit dimensions & letterbox padding
        val drawWidth: Float
        val drawHeight: Float
        val offsetX0: Float
        val offsetY0: Float

        if (containerAspect > imageAspect) {
            drawHeight = containerHeightPx
            drawWidth = containerHeightPx * imageAspect
            offsetX0 = (containerWidthPx - drawWidth) / 2f
            offsetY0 = 0f
        } else {
            drawWidth = containerWidthPx
            drawHeight = containerWidthPx / imageAspect
            offsetX0 = 0f
            offsetY0 = (containerHeightPx - drawHeight) / 2f
        }

        // --- 1. FULL-SCREEN INTERACTIVE MAP VIEW ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pinch-to-zoom and Drag-to-pan gesture listener
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        val (clampedX, clampedY) = clampPan(
                            scale = newScale,
                            panX = panX + pan.x,
                            panY = panY + pan.y,
                            drawWidth = drawWidth,
                            drawHeight = drawHeight,
                            containerWidthPx = containerWidthPx,
                            containerHeightPx = containerHeightPx,
                            topOverlayPx = topOverlayPx,
                            bottomOverlayPx = bottomOverlayPx
                        )
                        scale = newScale
                        panX = clampedX
                        panY = clampedY
                    }
                }
                .pointerInput(scale, panX, panY, isCalibrateMode, isGraphMode, isPickStartMode, locations, nodes, selectedNodeId, selectedLocation, topOverlayPx, bottomOverlayPx) {
                    // Tap gesture listener for selecting markers, calibration point, graph editing, or picking start
                    detectTapGestures { tapOffset ->
                        val tapX = tapOffset.x
                        val tapY = tapOffset.y

                        // Convert screen tap coordinate to relative image coordinate (0.0 to 1.0)
                        val imgX = (tapX - containerWidthPx / 2f - panX) / scale + containerWidthPx / 2f - offsetX0
                        val imgY = (tapY - containerHeightPx / 2f - panY) / scale + containerHeightPx / 2f - offsetY0

                        val relX = imgX / drawWidth
                        val relY = imgY / drawHeight

                        if (DEV_TOOLS_ENABLED && isCalibrateMode) {
                            // Calibrate mode tap handling (Dev Tools only)
                            if (relX in 0.0..1.0 && relY in 0.0..1.0) {
                                val roundX = round(relX * 1000.0) / 1000.0
                                val roundY = round(relY * 1000.0) / 1000.0
                                viewModel.setCalibrationTarget(roundX, roundY)
                            }
                        } else if (DEV_TOOLS_ENABLED && isGraphMode) {
                            // Graph Edit mode tap handling (Dev Tools only)
                            var hitNode: GraphNode? = null
                            for (node in nodes) {
                                val dx = (node.x - relX) * drawWidth
                                val dy = (node.y - relY) * drawHeight
                                val distPx = sqrt(dx * dx + dy * dy)
                                val hitRadiusDp = if (node.id == selectedNodeId) 16.dp else 10.dp
                                if (distPx <= (hitRadiusDp.toPx() / scale)) {
                                    hitNode = node
                                    break
                                }
                            }

                            if (hitNode != null) {
                                viewModel.handleNodeTap(hitNode.id)
                            } else if (relX in 0.0..1.0 && relY in 0.0..1.0) {
                                // Add a new graph node on empty map tap
                                viewModel.addNode(relX.toDouble(), relY.toDouble())
                            }
                        } else if (isPickStartMode) {
                            // Pick Start Mode tap handling: set nearest graph node as start
                            if (relX in 0.0..1.0 && relY in 0.0..1.0) {
                                val success = viewModel.pickNearestStartNode(relX.toDouble(), relY.toDouble(), imageWidth, imageHeight)
                                if (success) {
                                    Toast.makeText(context, "Start set to ${viewModel.startLabel.value}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Tap closer to a walkway", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Normal Mode: Check location marker hit test
                            var hitLocation: CampusLocation? = null
                            for (loc in locations) {
                                val dx = (loc.x - relX) * drawWidth
                                val dy = (loc.y - relY) * drawHeight
                                val distPx = sqrt(dx * dx + dy * dy)
                                val markerRadiusDp = if (loc.id == selectedLocation?.id) 20.dp else 12.dp
                                if (distPx <= (markerRadiusDp.toPx() / scale)) {
                                    hitLocation = loc
                                    break
                                }
                            }

                            if (hitLocation != null) {
                                viewModel.selectLocation(hitLocation)
                                isRoomsExpanded = false
                            } else {
                                viewModel.setDropdownExpanded(false)
                            }
                        }
                    }
                }
        ) {
            // Scaled and Panned Map Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panX
                        translationY = panY
                    }
            ) {
                // Background Map Image
                Image(
                    bitmap = mapBitmap,
                    contentDescription = "Campus Map",
                    modifier = Modifier
                        .offset { IntOffset(offsetX0.toInt(), offsetY0.toInt()) }
                        .size(
                            width = with(density) { drawWidth.toDp() },
                            height = with(density) { drawHeight.toDp() }
                        )
                )

                // Render Active Walking Route Polyline & Destination End Pin
                if (activeRoute != null && activeRoute!!.nodes.size >= 2) {
                    val routeNodes = activeRoute!!.nodes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val routePath = Path()
                        routeNodes.forEachIndexed { index, node ->
                            val nx = offsetX0 + (node.x.toFloat() * drawWidth)
                            val ny = offsetY0 + (node.y.toFloat() * drawHeight)
                            if (index == 0) {
                                routePath.moveTo(nx, ny)
                            } else {
                                routePath.lineTo(nx, ny)
                            }
                        }

                        drawPath(
                            path = routePath,
                            color = Color(0xFF1565C0), // Vibrant Royal Blue Route Line
                            style = Stroke(
                                width = 6.dp.toPx() / scale,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // End Route Pin (Crimson Red)
                    val lastNode = routeNodes.last()
                    val endX = offsetX0 + (lastNode.x.toFloat() * drawWidth)
                    val endY = offsetY0 + (lastNode.y.toFloat() * drawHeight)
                    val endMarkerSizeDp = 28.dp

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (endX - (endMarkerSizeDp.toPx() / 2f)).toInt(),
                                    y = (endY - (endMarkerSizeDp.toPx() / 2f)).toInt()
                                )
                            }
                            .size(endMarkerSizeDp)
                            .graphicsLayer {
                                scaleX = 1f / scale
                                scaleY = 1f / scale
                                transformOrigin = TransformOrigin.Center
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828)), // Crimson Red End Pin
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "End Pin",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Render Green Start Pin whenever startNodeId is set
                if (startNodeId != null) {
                    val startNode = nodes.find { it.id == startNodeId }
                    if (startNode != null) {
                        val startX = offsetX0 + (startNode.x.toFloat() * drawWidth)
                        val startY = offsetY0 + (startNode.y.toFloat() * drawHeight)
                        val startMarkerSizeDp = 28.dp

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (startX - (startMarkerSizeDp.toPx() / 2f)).toInt(),
                                        y = (startY - (startMarkerSizeDp.toPx() / 2f)).toInt()
                                    )
                                }
                                .size(startMarkerSizeDp)
                                .graphicsLayer {
                                    scaleX = 1f / scale
                                    scaleY = 1f / scale
                                    transformOrigin = TransformOrigin.Center
                                }
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)), // Green Start Pin
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Pin",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Render Graph Edges (Thin lines, ONLY in Graph Mode when Dev Tools are enabled)
                if (DEV_TOOLS_ENABLED && isGraphMode) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        edges.forEach { edge ->
                            val fromNode = nodes.find { it.id == edge.from }
                            val toNode = nodes.find { it.id == edge.to }
                            if (fromNode != null && toNode != null) {
                                val startX = offsetX0 + (fromNode.x.toFloat() * drawWidth)
                                val startY = offsetY0 + (fromNode.y.toFloat() * drawHeight)
                                val endX = offsetX0 + (toNode.x.toFloat() * drawWidth)
                                val endY = offsetY0 + (toNode.y.toFloat() * drawHeight)
                                drawLine(
                                    color = Color(0xFF0288D1),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2.dp.toPx() / scale
                                )
                            }
                        }
                    }
                }

                // Render Graph Nodes (Dots on map, ONLY in Graph Mode when Dev Tools are enabled)
                if (DEV_TOOLS_ENABLED && isGraphMode) {
                    nodes.forEach { node ->
                        val isNodeSelected = node.id == selectedNodeId
                        val nodeX = offsetX0 + (node.x.toFloat() * drawWidth)
                        val nodeY = offsetY0 + (node.y.toFloat() * drawHeight)
                        val nodeSizeDp = if (isNodeSelected) 16.dp else 10.dp

                        val nodeColor = when {
                            isNodeSelected -> Color(0xFFD32F2F)
                            node.locationId != null -> Color(0xFF388E3C)
                            else -> Color(0xFF0288D1)
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (nodeX - (nodeSizeDp.toPx() / 2f)).toInt(),
                                        y = (nodeY - (nodeSizeDp.toPx() / 2f)).toInt()
                                    )
                                }
                                .size(nodeSizeDp)
                                .graphicsLayer {
                                    scaleX = 1f / scale
                                    scaleY = 1f / scale
                                    transformOrigin = TransformOrigin.Center
                                }
                                .clip(CircleShape)
                                .background(if (isNodeSelected) Color(0xFFFFD700) else Color.White)
                                .padding(if (isNodeSelected) 2.dp else 1.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                                .clickable(enabled = isGraphMode) {
                                    viewModel.handleNodeTap(node.id)
                                }
                        )
                    }
                }

                // Location Markers
                locations.forEach { location ->
                    val isSelected = location.id == selectedLocation?.id

                    // Calculate marker position relative to image
                    val markerX = offsetX0 + (location.x.toFloat() * drawWidth)
                    val markerY = offsetY0 + (location.y.toFloat() * drawHeight)

                    val markerSizeDp = if (isSelected) 40.dp else 24.dp
                    val iconSizeDp = if (isSelected) 20.dp else 14.dp

                    // Category color coding
                    val markerColor = when (location.category.lowercase()) {
                        "building" -> MaterialTheme.colorScheme.primary
                        "gate" -> Color(0xFF1976D2)    // Blue
                        "canteen" -> Color(0xFFE65100) // Orange
                        "gym" -> Color(0xFF7B1FA2)     // Purple
                        "facility" -> Color(0xFF388E3C)// Forest Green
                        else -> MaterialTheme.colorScheme.secondary
                    }

                    // Show label for selected marker at any zoom, or for all markers when scale >= 2.0x (hidden in Calib/Graph/PickStart modes)
                    val isDebugActive = DEV_TOOLS_ENABLED && (isCalibrateMode || isGraphMode)
                    val shouldShowLabel = (!isDebugActive && !isPickStartMode) && (isSelected || scale >= 2.0f)

                    // 1. Render Marker Circle as its own Box (position is 100% fixed and identical regardless of label)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (markerX - (markerSizeDp.toPx() / 2f)).toInt(),
                                    y = (markerY - (markerSizeDp.toPx() / 2f)).toInt()
                                )
                            }
                            .size(markerSizeDp)
                            .graphicsLayer {
                                scaleX = 1f / scale
                                scaleY = 1f / scale
                                transformOrigin = TransformOrigin.Center
                            }
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFFFD700) else Color.White // Gold highlight for selected marker
                            )
                            .padding(if (isSelected) 4.dp else 2.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                            .clickable(enabled = !isDebugActive && !isPickStartMode) {
                                viewModel.selectLocation(location)
                                isRoomsExpanded = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = location.name,
                            tint = Color.White,
                            modifier = Modifier.size(iconSizeDp)
                        )
                    }

                    // 2. Render Name Label as a separate sibling element anchored below the marker circle
                    if (shouldShowLabel) {
                        val gapPx = with(density) { (markerSizeDp / 2f + 2.dp).toPx().roundToInt() }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = markerX.toInt(),
                                        y = markerY.toInt()
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = 1f / scale
                                    scaleY = 1f / scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                                .layout { measurable, _ ->
                                    val p = measurable.measure(Constraints())
                                    layout(0, 0) {
                                        p.place(-p.width / 2, gapPx)
                                    }
                                }
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(4.dp),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = location.name,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color(0xFF333333),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. FLOATING TOP SEARCH BAR, SWITCHES & CATEGORY CHIPS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Search Input Box with Calibrate and Graph Switches (Control switches rendered only when DEV_TOOLS_ENABLED is true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search location or room...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        if (DEV_TOOLS_ENABLED) {
                            Spacer(modifier = Modifier.width(8.dp))

                            // Control Mode Switches (Calibrate & Graph)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Calibrate Switch
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Calib",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCalibrateMode) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    Switch(
                                        checked = isCalibrateMode,
                                        onCheckedChange = { viewModel.toggleCalibrateMode(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Graph Switch
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Graph",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGraphMode) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    Switch(
                                        checked = isGraphMode,
                                        onCheckedChange = { viewModel.toggleGraphMode(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Category Chips Row
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category.equals(selectedCategory, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = { Text(category, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Top Banner for Pick Start Mode
            if (isPickStartMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TripOrigin,
                                contentDescription = "Pick Start",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tap the map to set your start",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        TextButton(
                            onClick = { viewModel.togglePickStartMode(false) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Floating Graph Edit Toolbar (Shown ONLY when DEV_TOOLS_ENABLED is true & Graph Mode is ON)
            if (DEV_TOOLS_ENABLED && isGraphMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Node & Edge Counter
                            Text(
                                text = "${nodes.size} nodes, ${edges.size} edges",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Undo Action Button
                                IconButton(
                                    onClick = { viewModel.undoGraphAction() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Undo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Delete Selected Node Button
                                IconButton(
                                    onClick = { viewModel.deleteSelectedNode() },
                                    enabled = selectedNodeId != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Node",
                                        tint = if (selectedNodeId != null) Color.Red else Color.LightGray
                                    )
                                }

                                // Copy Graph JSON Button
                                IconButton(
                                    onClick = {
                                        val jsonStr = viewModel.generateGraphJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Graph JSON", jsonStr)
                                        clipboard.setPrimaryClip(clip)
                                        Log.d("GraphJSON", jsonStr)
                                        Toast.makeText(context, "Copied Graph JSON to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy JSON",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Hint Line for Graph Mode
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap map: add + connect. Tap selected dot: end chain.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )

                        // Link Selected Node to Location Dropdown Row
                        if (selectedNodeId != null) {
                            val selectedNode = nodes.find { it.id == selectedNodeId }
                            val linkedLocation = locations.find { it.id == selectedNode?.locationId }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Node ${selectedNodeId}: ${linkedLocation?.name ?: "Unlinked"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )

                                var linkDropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { linkDropdownExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = if (linkedLocation != null) "Change Link" else "Link Loc", fontSize = 11.sp)
                                    }

                                    DropdownMenu(
                                        expanded = linkDropdownExpanded,
                                        onDismissRequest = { linkDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Unlink)") },
                                            onClick = {
                                                viewModel.linkSelectedNodeToLocation(null)
                                                linkDropdownExpanded = false
                                            }
                                        )
                                        locations.forEach { loc ->
                                            DropdownMenuItem(
                                                text = { Text(loc.name) },
                                                onClick = {
                                                    viewModel.linkSelectedNodeToLocation(loc.id)
                                                    linkDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dropdown List of Search Results
            if (isDropdownExpanded && searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.height(220.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { result ->
                            when (result) {
                                is SearchResult.LocationItem -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.selectLocation(result.location)
                                                viewModel.setDropdownExpanded(false)
                                                isRoomsExpanded = false

                                                // Center and zoom map onto selected location in visible viewport with bounds clamping
                                                val targetPx0 = offsetX0 + (result.location.x.toFloat() * drawWidth)
                                                val targetPy0 = offsetY0 + (result.location.y.toFloat() * drawHeight)
                                                val newScale = 3.0f
                                                val visibleCenterY = (containerHeightPx + topOverlayPx - bottomOverlayPx) / 2f
                                                val desiredPanX = -(targetPx0 - containerWidthPx / 2f) * newScale
                                                val desiredPanY = visibleCenterY - containerHeightPx / 2f - (targetPy0 - containerHeightPx / 2f) * newScale

                                                val (clampedX, clampedY) = clampPan(
                                                    scale = newScale,
                                                    panX = desiredPanX,
                                                    panY = desiredPanY,
                                                    drawWidth = drawWidth,
                                                    drawHeight = drawHeight,
                                                    containerWidthPx = containerWidthPx,
                                                    containerHeightPx = containerHeightPx,
                                                    topOverlayPx = topOverlayPx,
                                                    bottomOverlayPx = bottomOverlayPx
                                                )
                                                scale = newScale
                                                panX = clampedX
                                                panY = clampedY
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Location",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = result.location.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = result.location.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                                is SearchResult.RoomItem -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val building = viewModel.selectBuildingByRoom(result.room)
                                                viewModel.setDropdownExpanded(false)
                                                isRoomsExpanded = true

                                                if (building != null) {
                                                    val targetPx0 = offsetX0 + (building.x.toFloat() * drawWidth)
                                                    val targetPy0 = offsetY0 + (building.y.toFloat() * drawHeight)
                                                    val newScale = 3.0f
                                                    val visibleCenterY = (containerHeightPx + topOverlayPx - bottomOverlayPx) / 2f
                                                    val desiredPanX = -(targetPx0 - containerWidthPx / 2f) * newScale
                                                    val desiredPanY = visibleCenterY - containerHeightPx / 2f - (targetPy0 - containerHeightPx / 2f) * newScale

                                                    val (clampedX, clampedY) = clampPan(
                                                        scale = newScale,
                                                        panX = desiredPanX,
                                                        panY = desiredPanY,
                                                        drawWidth = drawWidth,
                                                        drawHeight = drawHeight,
                                                        containerWidthPx = containerWidthPx,
                                                        containerHeightPx = containerHeightPx,
                                                        topOverlayPx = topOverlayPx,
                                                        bottomOverlayPx = bottomOverlayPx
                                                    )
                                                    scale = newScale
                                                    panX = clampedX
                                                    panY = clampedY
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoorFront,
                                            contentDescription = "Room",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = result.room.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "${result.buildingName} • Floor ${result.room.floor}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. FLOATING ACTION BUTTONS (Zoom In, Zoom Out, Reset View) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val newScale = (scale + 0.5f).coerceIn(1f, 5f)
                    val (clampedX, clampedY) = clampPan(
                        scale = newScale,
                        panX = panX,
                        panY = panY,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        containerWidthPx = containerWidthPx,
                        containerHeightPx = containerHeightPx,
                        topOverlayPx = topOverlayPx,
                        bottomOverlayPx = bottomOverlayPx
                    )
                    scale = newScale
                    panX = clampedX
                    panY = clampedY
                },
                modifier = Modifier.size(44.dp),
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = {
                    val newScale = (scale - 0.5f).coerceIn(1f, 5f)
                    val (clampedX, clampedY) = clampPan(
                        scale = newScale,
                        panX = panX,
                        panY = panY,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        containerWidthPx = containerWidthPx,
                        containerHeightPx = containerHeightPx,
                        topOverlayPx = topOverlayPx,
                        bottomOverlayPx = bottomOverlayPx
                    )
                    scale = newScale
                    panX = clampedX
                    panY = clampedY
                },
                modifier = Modifier.size(44.dp),
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = {
                    scale = 1f
                    panX = 0f
                    panY = topOverlayPx / 2f
                    viewModel.selectLocation(null)
                    isRoomsExpanded = false
                },
                modifier = Modifier.size(44.dp),
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset View")
            }
        }

        // --- 4. BOTTOM LOCATION DETAILS CARD ---
        if (selectedLocation != null) {
            val loc = selectedLocation!!
            val buildingRooms = remember(loc.id) {
                if (loc.category.equals("Building", ignoreCase = true)) {
                    viewModel.getRoomsByBuilding(loc.id)
                } else {
                    emptyList()
                }
            }

            var cardOffsetY by remember(loc.id) { mutableFloatStateOf(0f) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .offset { IntOffset(0, cardOffsetY.roundToInt()) }
                    .pointerInput(loc.id) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (cardOffsetY > 60.dp.toPx()) {
                                    viewModel.selectLocation(null)
                                    isRoomsExpanded = false
                                }
                                cardOffsetY = 0f
                            },
                            onDragCancel = {
                                cardOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                cardOffsetY = (cardOffsetY + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Location Name, Category, & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = loc.category,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Show Start Location Badge if this is current start location
                                if (startNodeId != null && startLabel == loc.name) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Start Location",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = {
                            viewModel.selectLocation(null)
                            isRoomsExpanded = false
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Display Start Location Subtitle
                    if (startNodeId != null && startLabel != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TripOrigin,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Start: $startLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.clearStartLocation() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Start",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Route Summary Info (Shown ONLY for the target destination)
                    if (activeRoute != null && routeDestinationId == loc.id) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${round(activeRoute!!.totalMeters).toInt()} m, about ${activeRoute!!.durationMinutes} min",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    } else if (routeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = routeError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Hide description and action buttons when in Graph mode to keep card compact
                    if (!DEV_TOOLS_ENABLED || !isGraphMode) {
                        if (activeRoute == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loc.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Horizontally Scrollable Action Buttons Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Set as start" Button
                            OutlinedButton(
                                onClick = {
                                    val ok = viewModel.setStartLocation(loc)
                                    if (ok) {
                                        Toast.makeText(context, "Set ${loc.name} as start", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Location not connected to map graph", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TripOrigin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set as start", fontSize = 12.sp, maxLines = 1)
                            }

                            // "Pick start" Button
                            OutlinedButton(
                                onClick = {
                                    if (!isCalibrateMode && !isGraphMode) {
                                        viewModel.togglePickStartMode(true)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick start", fontSize = 12.sp, maxLines = 1)
                            }

                            // "Directions" Button
                            Button(
                                onClick = {
                                    viewModel.calculateRoute(loc, imageWidth, imageHeight)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", fontSize = 12.sp, maxLines = 1)
                            }

                            // "Clear route" Button (shown when route is active)
                            if (activeRoute != null) {
                                OutlinedButton(
                                    onClick = { viewModel.clearRoute() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear route", fontSize = 12.sp, maxLines = 1)
                                }
                            }

                            // "View rooms" Button
                            if (loc.category.equals("Building", ignoreCase = true) && buildingRooms.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { isRoomsExpanded = !isRoomsExpanded },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Rooms (${buildingRooms.size})", fontSize = 12.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isRoomsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                        // Expanded Room List grouped by floor
                        AnimatedVisibility(visible = isRoomsExpanded && buildingRooms.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = "Building Rooms",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                val groupedRooms = buildingRooms.groupBy { it.floor }
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupedRooms.forEach { (floor, roomsOnFloor) ->
                                        item {
                                            Text(
                                                text = "Floor $floor",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                            roomsOnFloor.forEach { room ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp, horizontal = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "• ${room.name}",
                                                        modifier = Modifier.weight(1f),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        text = room.type,
                                                        modifier = Modifier.padding(start = 8.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray,
                                                        softWrap = false,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4b. FLOATING ROUTE SUMMARY CHIP (shown when bottom card is closed) ---
        if (activeRoute != null && selectedLocation == null) {
            val destLoc = locations.find { it.id == routeDestinationId }
            val destName = destLoc?.name ?: "Destination"
            val routeMeters = round(activeRoute!!.totalMeters).toInt()
            val routeMin = activeRoute!!.durationMinutes

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            if (destLoc != null) {
                                viewModel.selectLocation(destLoc)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$destName: $routeMeters m, about $routeMin min",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.clearRoute() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Route",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 5. CALIBRATION DIALOG ---
        if (DEV_TOOLS_ENABLED && calibrationTarget != null) {
            val target = calibrationTarget!!
            var selectedLocationForCalib by remember { mutableStateOf(locations.firstOrNull()) }

            AlertDialog(
                onDismissRequest = { viewModel.clearCalibrationTarget() },
                title = { Text("Calibrate Location") },
                text = {
                    Column {
                        Text(
                            text = "Tapped Coordinates:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "x: ${target.first}, y: ${target.second}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Location:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        // Location Selection Dropdown Menu
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedLocationForCalib?.name ?: "Select location",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                locations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc.name) },
                                        onClick = {
                                            selectedLocationForCalib = loc
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val locId = selectedLocationForCalib?.id ?: "unknown"
                            val jsonString = "{\"id\":\"$locId\",\"x\":${target.first},\"y\":${target.second}}"

                            // Copy JSON to system Clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Campus Location Calib", jsonString)
                            clipboard.setPrimaryClip(clip)

                            // Log JSON string
                            Log.d("Calibrate", jsonString)

                            Toast.makeText(context, "Copied to clipboard!\n$jsonString", Toast.LENGTH_LONG).show()
                            viewModel.clearCalibrationTarget()
                        }
                    ) {
                        Text("Copy JSON")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearCalibrationTarget() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
