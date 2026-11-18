package com.example.rutaarasof.data.model

import kotlin.math.ceil

// Developer tools toggle (set to true to enable Calibrate and Graph Edit tools)
const val DEV_TOOLS_ENABLED = false

// Calibratable map scale constants
const val METERS_PER_PIXEL = 0.0777f // Calibrated from a Google Maps measurement (Open Court to Lian Cafeteria, 56.98 m) over about 733 map pixels.
const val WALKING_SPEED_MPS = 1.3f    // Average walking speed: 1.3 meters / second

// Data class representing a calculated walking route result
data class RouteResult(
    val nodes: List<GraphNode>,
    val totalMeters: Float
) {
    // Calculated estimated walking time in minutes (rounded up)
    val durationMinutes: Int
        get() {
            val seconds = totalMeters / WALKING_SPEED_MPS
            return ceil(seconds / 60.0).toInt().coerceAtLeast(1)
        }
}
