package pt.isec.a2023131593.AMovProjetoKotlin.model

enum class AlertType(val field: String, val title: String) {
    PANIC("PANIC", "PANIC"),
    FALL("FALL", "FALL"),
    ACCIDENT("ACCIDENT", "ACCIDENT"),
    INACTIVITY("INACTIVITY", "INACTIVITY"),
    SPEED("SPEED", "SPEED"),
    GEOFENCING("LOCATION", "LOCATION")
}