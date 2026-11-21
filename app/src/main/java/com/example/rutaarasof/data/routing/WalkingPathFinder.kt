package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.METERS_PER_PIXEL
import kotlin.math.sqrt

/**
 * Path finder that calculates edge costs as real-world walking distance in meters.
 */
class WalkingPathFinder : BasePathFinder() {

    override fun edgeCost(
        a: GraphNode,
        b: GraphNode,
        mapWidth: Float,
        mapHeight: Float
    ): Float {
        val dx = (b.x - a.x).toFloat() * mapWidth
        val dy = (b.y - a.y).toFloat() * mapHeight
        val distPx = sqrt(dx * dx + dy * dy)
        return distPx * METERS_PER_PIXEL
    }
}
