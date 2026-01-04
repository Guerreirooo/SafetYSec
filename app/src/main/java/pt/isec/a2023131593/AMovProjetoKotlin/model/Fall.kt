package pt.isec.a2023131593.AMovProjetoKotlin.model

import pt.isec.a2023131593.AMovProjetoKotlin.model.UserData

fun checkFall(userData: UserData, parameters: List<String> = emptyList()): Boolean {
    return userData.fallDetected
}