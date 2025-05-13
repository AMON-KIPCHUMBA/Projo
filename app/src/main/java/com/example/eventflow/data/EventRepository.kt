package com.example.eventflow.data

import com.example.eventflow.models.EventModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class EventRepository {
    val database = FirebaseDatabase.getInstance().getReference("Events")
    private val auth = FirebaseAuth.getInstance()
    private val _events = MutableStateFlow<List<EventModel>>(emptyList())
    val events: StateFlow<List<EventModel>> = _events

    fun getCurrentUserId(): String? {
        val uid = auth.currentUser?.uid
        println("EventRepository: Current user ID=$uid")
        return uid
    }

    fun fetchEvents(userId: String) {
        database.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventList = mutableListOf<EventModel>()
                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(EventModel::class.java)
                    event?.let { eventList.add(it) }
                }
                _events.value = eventList
                println("EventRepository: Fetched ${eventList.size} events for userId=$userId")
            }

            override fun onCancelled(error: DatabaseError) {
                println("EventRepository: Fetch events cancelled, error=${error.message}")
            }
        })
    }

    suspend fun saveEvent(userId: String, event: EventModel): Result<Unit> {
        return try {
            val eventId = event.eventId.ifEmpty { database.child(userId).push().key ?: "" }
            val updatedEvent = event.copy(eventId = eventId, userId = userId)
            database.child(userId).child(eventId).setValue(updatedEvent).await()
            println("EventRepository: Saved event, eventId=$eventId, title=${event.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("EventRepository: Failed to save event, error=${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(userId: String, eventId: String): Result<Unit> {
        return try {
            database.child(userId).child(eventId).removeValue().await()
            println("EventRepository: Deleted event, eventId=$eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("EventRepository: Failed to delete event, error=${e.message}")
            Result.failure(e)
        }
    }
}