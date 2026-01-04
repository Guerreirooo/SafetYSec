package pt.isec.a2023131593.AMovProjetoKotlin.model

data class UserData(
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val fallDetected: Boolean,
    val accidentDetected: Boolean,
    val lastMovementTimestamp: Long
)