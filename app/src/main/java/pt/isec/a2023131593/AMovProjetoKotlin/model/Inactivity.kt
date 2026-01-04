package pt.isec.a2023131593.AMovProjetoKotlin.model

import pt.isec.a2023131593.AMovProjetoKotlin.model.UserData

fun checkInactivity(userData: UserData, parameters: List<String>): Boolean {
    val durationMinutes = parameters.firstOrNull()?.toIntOrNull() ?: return false
    val elapsedMillis = System.currentTimeMillis() - userData.lastMovementTimestamp
    return elapsedMillis >= durationMinutes * 60 * 1000
}