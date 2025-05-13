package com.example.eventflow.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.eventflow.data.EventRepository
import com.example.eventflow.models.EventModel
import com.example.eventflow.navigation.ROUTE_EVENT_LIST
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EventViewModel : ViewModel() {
    private val repository = EventRepository()
    val events: StateFlow<List<EventModel>> = repository.events
    private val wishlist = mutableListOf<EventModel>()

    fun fetchEvents(userId: String) {
        repository.fetchEvents(userId)
    }

    suspend fun getEventById(eventId: String): EventModel? {
        // Check cached events first
        val cachedEvent = events.value.find { it.eventId == eventId }
        if (cachedEvent != null) {
            println("EventViewModel: Found event in cache, eventId=$eventId, title=${cachedEvent.title}")
            return cachedEvent
        }

        // Fetch from Firebase (assumes userId is known)
        return try {
            val userId = repository.getCurrentUserId() ?: return null
            val snapshot = repository.database.child(userId).child(eventId).get().await()
            val event = snapshot.getValue(EventModel::class.java)
            println("EventViewModel: Fetched event from Firebase, eventId=$eventId, event=${event?.title}")
            event
        } catch (e: Exception) {
            println("EventViewModel: Error fetching event, eventId=$eventId, error=${e.message}")
            null
        }
    }

    fun isEventInWishlist(eventId: String): Boolean {
        return wishlist.any { it.eventId == eventId }
    }

    suspend fun addToWishlist(event: EventModel): Result<Unit> {
        return try {
            wishlist.add(event)
            println("EventViewModel: Added to wishlist, eventId=${event.eventId}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("EventViewModel: Failed to add to wishlist, error=${e.message}")
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(eventId: String): Result<Unit> {
        return try {
            wishlist.removeIf { it.eventId == eventId }
            println("EventViewModel: Removed from wishlist, eventId=$eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("EventViewModel: Failed to remove from wishlist, error=${e.message}")
            Result.failure(e)
        }
    }

    fun saveEvent(
        event: EventModel,
        userId: String,
        navController: NavController,
        context: Context
    ) {
        if (event.title.isBlank() || event.date.isBlank() || event.time.isBlank()) {
            Toast.makeText(context, "Please fill in title, date, and time", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            val result = repository.saveEvent(userId, event)
            if (result.isSuccess) {
                Toast.makeText(context, "Event saved successfully", Toast.LENGTH_LONG).show()
                navController.navigate(ROUTE_EVENT_LIST)
            } else {
                Toast.makeText(context, "Failed to save event", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteEvent(
        userId: String,
        eventId: String,
        context: Context
    ) {
        viewModelScope.launch {
            val result = repository.deleteEvent(userId, eventId)
            if (result.isSuccess) {
                Toast.makeText(context, "Event deleted successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_LONG).show()
            }
        }
    }
}