package pt.isec.a2023131593.AMovProjetoKotlin.model.enums

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class HistoryItem(
    val type: String,
    val date: Timestamp,
    val location: GeoPoint,
    val codeWritted: Boolean
)