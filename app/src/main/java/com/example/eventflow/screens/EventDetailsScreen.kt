package com.example.eventflow.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventflow.models.EventModel
import com.example.eventflow.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EventDetailsScreen(navController: NavController, eventId: String, eventViewModel: EventViewModel) {
    var event by remember { mutableStateOf<EventModel?>(null) }
    var isInWishlist by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wishlistAdded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Heart animation state
    val wishlistTransition = updateTransition(targetState = wishlistAdded, label = "wishlist")
    val heartScale by wishlistTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "heartScale"
    ) { if (it) 1.2f else 1.0f }

    // Custom back handler
    BackHandler(enabled = true) {
        println("EventDetailsScreen: Back button pressed, popping back stack")
        navController.popBackStack()
    }

    LaunchedEffect(eventId) {
        println("EventDetailsScreen: Started with eventId=$eventId")

        if (eventId.isBlank()) {
            println("EventDetailsScreen: Empty eventId")
            errorMessage = "Invalid event ID"
            isLoading = false
            return@LaunchedEffect
        }

        // Fetch event from ViewModel
        val eventResult = withTimeoutOrNull(5000L) {
            withContext(Dispatchers.IO) {
                println("EventDetailsScreen: Fetching event for eventId=$eventId")
                eventViewModel.getEventById(eventId)
            }
        }

        when {
            eventResult == null -> {
                println("EventDetailsScreen: Event fetch timed out")
                errorMessage = "Request timed out. Please check your connection."
            }
            eventResult != null -> {
                event = eventResult
                println("EventDetailsScreen: Event fetch success, event=${event?.title}")
                if (event == null) {
                    errorMessage = "Event not found"
                    println("EventDetailsScreen: Event not found for eventId=$eventId")
                }
            }
        }

        // Check wishlist status
        if (event != null) {
            isInWishlist = eventViewModel.isEventInWishlist(eventId)
            wishlistAdded = isInWishlist
            println("EventDetailsScreen: Wishlist status, isInWishlist=$isInWishlist")
        }

        isLoading = false
        println("EventDetailsScreen: Loading complete, isLoading=$isLoading, event=${event?.title}, errorMessage=$errorMessage")
    }

    // Format price as KSH
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
        currency = Currency.getInstance("KES")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            event?.let { eventData ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = !wishlistAdded,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(300))
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = eventViewModel.addToWishlist(eventData)
                                        if (result.isSuccess) {
                                            isInWishlist = true
                                            wishlistAdded = true
                                            snackbarHostState.showSnackbar("Added to wishlist")
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Failed: ${result.exceptionOrNull()?.message}"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(heartScale),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Add to Wishlist",
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wishlist")
                            }
                        }

                        Button(
                            onClick = { navController.navigate("booking_confirmation/${eventData.eventId}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Book Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                visible = !isLoading && errorMessage != null,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }

            AnimatedVisibility(
                visible = !isLoading && event != null && errorMessage == null,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                event?.let { eventData ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero image
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            AsyncImage(
                                model = "https://via.placeholder.com/600x200?text=${eventData.title}",
                                contentDescription = "Event Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Event details card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = eventData.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Date: ${eventData.date}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Time: ${eventData.time}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Location: ${eventData.location}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Price: ${currencyFormat.format(eventData.price)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = eventData.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}