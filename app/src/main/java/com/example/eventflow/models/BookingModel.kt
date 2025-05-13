package com.example.eventflow.models

data class BookingModel(
    val bookingId: String = "",
    val eventId: String = "",
    val userId: String = "",
    val eventTitle: String = "",
    val price: Double = 0.0,
    val bookingDate: String = "", // e.g., "2025-05-10"
    val status: String = "Confirmed" // e.g., "Confirmed", "Pending"
)