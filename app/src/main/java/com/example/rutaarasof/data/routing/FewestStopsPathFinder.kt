package com.example.rutaarasof.data.routing

import com.example.rutaarasof.data.model.GraphNode

/**
 * Concrete implementation of [BasePathFinder] that assigns a uniform cost of 1.0f to every edge,
 * finding the path with the fewest number of node stops / hops. Demonstrates OOP polymorphism.
 */
class FewestStopsPathFinder : BasePathFinder() {

    /**
     * Overrides [edgeCost] to return a constant unit cost of 1.0f for every edge.
     */
    override fun edgeCost(
        a: GraphNode,
        b: GraphNode,
        mapWidth: Float,
        mapHeight: Float
    ): Float {
        return 1f
    }
}
