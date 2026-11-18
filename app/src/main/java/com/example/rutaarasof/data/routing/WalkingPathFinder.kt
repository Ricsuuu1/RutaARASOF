package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphNode
import com.example.rutaarasof.data.model.METERS_PER_PIXEL
import kotlin.math.sqrt

/**
 * Concrete implementation of [BasePathFinder] that computes edge costs as real-world walking distance in meters.
 */
class WalkingPathFinder : BasePathFinder() {

    /**
     * Overrides [edgeCost] to calculate Euclidean pixel distance between nodes [a] and [b]
     * scaled to real-world meters using [METERS_PER_PIXEL].
     */
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
