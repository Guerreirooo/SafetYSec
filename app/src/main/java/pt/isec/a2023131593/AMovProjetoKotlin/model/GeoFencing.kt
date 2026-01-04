package pt.isec.a2023131593.AMovProjetoKotlin.rules

import pt.isec.a2023131593.AMovProjetoKotlin.model.UserData
import kotlin.math.*

fun checkGeofencing(userData: UserData, parameters: List<String>): Boolean {
    if (parameters.size < 3) return false

    val lat = parameters[0].toDoubleOrNull() ?: return false
    val lng = parameters[1].toDoubleOrNull() ?: return false
    val radius = parameters[2].toFloatOrNull() ?: return false

    val distance = distanceBetween(userData.lat, userData.lng, lat, lng)
    return distance > radius
}

private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371000 // metros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (R * c).toFloat()
}


