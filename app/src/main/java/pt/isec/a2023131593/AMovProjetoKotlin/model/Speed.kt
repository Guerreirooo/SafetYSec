package pt.isec.a2023131593.AMovProjetoKotlin.model

import pt.isec.a2023131593.AMovProjetoKotlin.model.UserData

fun checkSpeed(userData: UserData, parameters: List<String>): Boolean {
    val maxSpeed = parameters.firstOrNull()?.toFloatOrNull() ?: return false
    return userData.speed > maxSpeed
}
