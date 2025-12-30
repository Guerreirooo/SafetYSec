package pt.isec.a2023131593.AMovProjetoKotlin.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val cancelCode: String = "1234"
)