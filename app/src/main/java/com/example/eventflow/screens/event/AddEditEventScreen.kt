package com.example.eventflow.screens.event

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eventflow.data.AuthRepository
import com.example.eventflow.models.EventModel
import com.example.eventflow.viewmodel.EventViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(navController: NavController, eventViewModel: EventViewModel, eventIdArg: String?) {
    val context = LocalContext.current
    val authRepository = AuthRepository()
    val userId = authRepository.getCurrentUserId()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for event fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var eventId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load event data if editing
    LaunchedEffect(eventIdArg) {
        if (!eventIdArg.isNullOrEmpty()) {
            isLoading = true
            val event = eventViewModel.getEventById(eventIdArg)
            event?.let {
                title = it.title
                description = it.description
                date = it.date
                time = it.time
                location = it.location
                price = if (it.price > 0) it.price.toString() else ""
                eventId = it.eventId
            } ?: run {
                errorMessage = "Event not found"
            }
            isLoading = false
        }
    }

    // Input validation
    fun validateInputs(): String? {
        if (title.isBlank()) return "Title is required"
        if (date.isBlank()) return "Date is required"
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return "Date must be YYYY-MM-DD"
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                ?: return "Invalid date format"
        } catch (e: Exception) {
            return "Invalid date"
        }
        if (time.isBlank()) return "Time is required"
        if (!time.matches(Regex("\\d{2}:\\d{2}"))) return "Time must be HH:MM"
        if (price.isNotBlank() && price.toDoubleOrNull() == null) return "Invalid price"
        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (eventId.isEmpty()) "Create Event" else "Edit Event",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (userId == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please log in to save event")
                        }
                        return@FloatingActionButton
                    }
                    val validationError = validateInputs()
                    if (validationError != null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(validationError)
                        }
                        return@FloatingActionButton
                    }
                    isLoading = true
                    val event = EventModel(
                        eventId = eventId,
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        location = location,
                        userId = userId,
                        price = price.toDoubleOrNull() ?: 0.0
                    )
                    eventViewModel.saveEvent(event, userId, navController, context)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Event",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                visible = !isLoading && errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
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
                visible = !isLoading && errorMessage == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Event Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title") },
                            leadingIcon = {
                                Icon(Icons.Default.Event, contentDescription = "Title")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = title.isBlank() && errorMessage != null,
                            supportingText = if (title.isBlank() && errorMessage != null) {
                                { Text("Title is required") }
                            } else null
                        )

                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = date.isBlank() && errorMessage != null,
                            supportingText = if (date.isBlank() && errorMessage != null) {
                                { Text("Date is required") }
                            } else null
                        )

                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (HH:MM)") },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = "Time")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = time.isBlank() && errorMessage != null,
                            supportingText = if (time.isBlank() && errorMessage != null) {
                                { Text("Time is required") }
                            } else null
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = "Location")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price (KSH)") },
                            leadingIcon = {
                                Icon(Icons.Default.Money, contentDescription = "Price")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = price.isNotBlank() && price.toDoubleOrNull() == null,
                            supportingText = if (price.isNotBlank() && price.toDoubleOrNull() == null) {
                                { Text("Invalid price") }
                            } else null
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = "Description")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4
                        )
                    }
                }
            }
        }
    }
}