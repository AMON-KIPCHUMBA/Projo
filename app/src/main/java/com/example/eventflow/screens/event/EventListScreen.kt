package com.example.eventflow.screens.event

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eventflow.data.AuthRepository
import com.example.eventflow.models.EventModel
import com.example.eventflow.navigation.ROUTE_ADD_EDIT_EVENT
import com.example.eventflow.navigation.ROUTE_EVENT_DETAILS
import com.example.eventflow.viewmodel.EventViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(navController: NavController, eventViewModel: EventViewModel) {
    val context = LocalContext.current
    val events = eventViewModel.events.collectAsState().value
    val authRepository = AuthRepository()
    val userId = authRepository.getCurrentUserId() ?: return
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for deletion confirmation
    val showDeleteDialog = remember { mutableStateOf(false) }
    val eventToDelete = remember { mutableStateOf<EventModel?>(null) }

    // Animation states
    val isLoading = remember { mutableStateOf(true) }

    // Fetch events when the screen is displayed
    LaunchedEffect(Unit) {
        eventViewModel.fetchEvents(userId)
        delay(500)
        isLoading.value = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "My Events",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(ROUTE_ADD_EDIT_EVENT) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading.value) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (events.isEmpty()) {
                    EmptyEventsList()
                } else {
                    EventsList(
                        events = events,
                        onEventClick = { event ->
                            navController.navigate("event_details/${event.eventId}")
                        },
                        onDeleteClick = { event ->
                            eventToDelete.value = event
                            showDeleteDialog.value = true
                        },
                        onWishlistClick = { event ->
                            scope.launch {
                                val result = authRepository.addToWishlist(userId, event)
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Added to wishlist")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to add to wishlist")
                                }
                            }
                        }
                    )
                }

                // Delete Confirmation Dialog
                if (showDeleteDialog.value && eventToDelete.value != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        title = { Text("Delete Event") },
                        text = { Text("Are you sure you want to delete '${eventToDelete.value?.title}'?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    eventToDelete.value?.let { event ->
                                        scope.launch {
                                            eventViewModel.deleteEvent(userId, event.eventId, context)
                                            showDeleteDialog.value = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeleteDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun EmptyEventsList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No events yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to create your first event",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun EventsList(
    events: List<EventModel>,
    onEventClick: (EventModel) -> Unit,
    onDeleteClick: (EventModel) -> Unit,
    onWishlistClick: (EventModel) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(events) { index, event ->
            val animDelay = 50 * index
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(animDelay.toLong())
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(500)
                        ),
                exit = fadeOut()
            ) {
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    onDeleteClick = { onDeleteClick(event) },
                    onWishlistClick = { onWishlistClick(event) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: EventModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onWishlistClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onWishlistClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Add to Wishlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${event.date} at ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!event.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}