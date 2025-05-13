package com.example.eventflow.models

data class EventModel(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val userId: String = "",
    val price: Double = 0.0 // New field for event cost
)