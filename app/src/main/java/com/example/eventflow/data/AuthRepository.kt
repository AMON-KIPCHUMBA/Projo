package com.example.eventflow.data

import com.example.eventflow.models.BookingModel
import com.example.eventflow.models.EventModel
import com.example.eventflow.models.UserModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Users")
    private val eventsDatabase = FirebaseDatabase.getInstance().getReference("Events")
    private val wishlistsDatabase = FirebaseDatabase.getInstance().getReference("Wishlists")
    private val bookingsDatabase = FirebaseDatabase.getInstance().getReference("Bookings")

    suspend fun signUp(email: String, password: String, name: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User ID not found"))
            val user = UserModel(userId, name, email)
            database.child(userId).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

    @Suppress("DEPRECATION")
    suspend fun updateUserProfile(
        userId: String,
        name: String,
        email: String,
        password: String?
    ): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            if (email != user.email) {
                user.updateEmail(email).await()
            }
            if (!password.isNullOrEmpty()) {
                user.updatePassword(password).await()
            }
            database.child(userId).child("name").setValue(name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(userId: String, password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val credential = EmailAuthProvider.getCredential(user.email ?: "", password)
            user.reauthenticate(credential).await()
            database.child(userId).removeValue().await()
            eventsDatabase.child(userId).removeValue().await()
            wishlistsDatabase.child(userId).removeValue().await()
            bookingsDatabase.child(userId).removeValue().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserData(userId: String): Flow<UserModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserModel::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null) // Handle error gracefully
            }
        }
        database.child(userId).addValueEventListener(listener)
        awaitClose { database.child(userId).removeEventListener(listener) }
    }

    suspend fun getEventById(userId: String, eventId: String): Result<EventModel> {
        return try {
            val snapshot = eventsDatabase.child(userId).child(eventId).get().await()
            val event = snapshot.getValue(EventModel::class.java)
            if (event != null) {
                Result.success(event)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToWishlist(userId: String, event: EventModel): Result<Unit> {
        return try {
            wishlistsDatabase.child(userId).child(event.eventId).setValue(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(userId: String, eventId: String): Result<Unit> {
        return try {
            wishlistsDatabase.child(userId).child(eventId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWishlist(userId: String): Flow<List<EventModel>> {
        if (auth.currentUser == null || auth.currentUser?.uid != userId) {
            return flowOf(emptyList()) // Return empty list if not authenticated
        }
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val events = mutableListOf<EventModel>()
                    for (child in snapshot.children) {
                        val event = child.getValue(EventModel::class.java)
                        event?.let { events.add(it) }
                    }
                    trySend(events)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList()) // Handle error gracefully
                }
            }
            wishlistsDatabase.child(userId).addValueEventListener(listener)
            awaitClose { wishlistsDatabase.child(userId).removeEventListener(listener) }
        }
    }

    suspend fun createBooking(userId: String, event: EventModel): Result<BookingModel> {
        return try {
            val bookingId = bookingsDatabase.child(userId).push().key ?: throw Exception("Booking ID generation failed")
            val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val booking = BookingModel(
                bookingId = bookingId,
                eventId = event.eventId,
                userId = userId,
                eventTitle = event.title,
                price = event.price,
                bookingDate = bookingDate,
                status = "Confirmed"
            )
            bookingsDatabase.child(userId).child(bookingId).setValue(booking).await()
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBookings(userId: String): Flow<List<BookingModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = mutableListOf<BookingModel>()
                for (child in snapshot.children) {
                    val booking = child.getValue(BookingModel::class.java)
                    booking?.let { bookings.add(it) }
                }
                trySend(bookings)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList()) // Handle error gracefully
            }
        }
        bookingsDatabase.child(userId).addValueEventListener(listener)
        awaitClose { bookingsDatabase.child(userId).removeEventListener(listener) }
    }

    // Add getEvents for EventViewModel
    fun getEvents(userId: String): Flow<List<EventModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val events = mutableListOf<EventModel>()
                for (child in snapshot.children) {
                    child.getValue(EventModel::class.java)?.let { events.add(it) }
                }
                trySend(events)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        eventsDatabase.child(userId).addValueEventListener(listener)
        awaitClose { eventsDatabase.child(userId).removeEventListener(listener) }
    }

    suspend fun deleteEvent(userId: String, eventId: String): Result<Unit> {
        return try {
            eventsDatabase.child(userId).child(eventId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}