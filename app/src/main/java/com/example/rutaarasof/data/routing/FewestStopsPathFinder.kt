package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphNode

/**
 * Path finder that assigns a uniform cost to every edge to find paths with the fewest stops.
 */
class FewestStopsPathFinder : BasePathFinder() {

    override fun edgeCost(
        a: GraphNode,
        b: GraphNode,
        mapWidth: Float,
        mapHeight: Float
    ): Float {
        return 1f
    }
}
