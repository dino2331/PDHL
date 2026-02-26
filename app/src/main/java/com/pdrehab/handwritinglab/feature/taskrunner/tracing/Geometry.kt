package com.pdrehab.handwritinglab.feature.taskrunner.tracing

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun distPointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val vx = bx - ax
    val vy = by - ay
    val wx = px - ax
    val wy = py - ay
    val c1 = vx * wx + vy * wy
    if (c1 <= 0f) return sqrt((px - ax)*(px - ax) + (py - ay)*(py - ay))
    val c2 = vx * vx + vy * vy
    if (c2 <= c1) return sqrt((px - bx)*(px - bx) + (py - by)*(py - by))
    val t = c1 / c2
    val projX = ax + t * vx
    val projY = ay + t * vy
    return sqrt((px - projX)*(px - projX) + (py - projY)*(py - projY))
}

fun minDistToPolyline(px: Float, py: Float, pts: List<Pair<Float, Float>>): Float {
    if (pts.size < 2) return Float.NaN
    var best = Float.POSITIVE_INFINITY
    for (i in 1 until pts.size) {
        val (ax, ay) = pts[i - 1]
        val (bx, by) = pts[i]
        val d = distPointToSegment(px, py, ax, ay, bx, by)
        if (d < best) best = d
    }
    return best
}