package com.example.eventflow.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventflow.models.EventModel
import com.example.eventflow.viewmodel.EventViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(navController: NavController, eventId: String, eventViewModel: EventViewModel) {
    var event by remember { mutableStateOf<EventModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Format price as KSH
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
        currency = Currency.getInstance("KES")
    }

    // Back button handling
    BackHandler {
        println("BookingConfirmationScreen: Back button pressed, popping back stack")
        navController.popBackStack()
    }

    LaunchedEffect(eventId) {
        if (eventId.isBlank()) {
            errorMessage = "Invalid event ID"
            isLoading = false
            println("BookingConfirmationScreen: Invalid eventId")
            return@LaunchedEffect
        }

        val eventResult = eventViewModel.getEventById(eventId)
        if (eventResult != null) {
            event = eventResult
            println("BookingConfirmationScreen: Event fetch success, event=${event?.title}")
        } else {
            errorMessage = "Event not found"
            println("BookingConfirmationScreen: Event not found for eventId=$eventId")
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Event", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
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
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                event?.let { eventData ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Event image
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

                        // Booking details
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
                                    text = "Booking Confirmation",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Event: ${eventData.title}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Date: ${eventData.date} at ${eventData.time}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Location: ${eventData.location}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Price: ${currencyFormat.format(eventData.price)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Payment Status: Pending",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Pay with SIM Toolkit
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Try to directly open SIM Toolkit app first
                                    val packageManager = navController.context.packageManager
                                    val simToolkitPackages = listOf(
                                        "com.android.stk",           // Generic SIM Toolkit
                                        "com.safaricom.stk",         // Safaricom SIM Toolkit
                                        "com.android.phone.stk",     // Another common SIM Toolkit package
                                        "com.android.simapplication" // Alternative SIM Application package
                                    )

                                    var launchSuccess = false

                                    // Try each package until one works
                                    for (packageName in simToolkitPackages) {
                                        try {
                                            // Check if the package exists
                                            packageManager.getPackageInfo(packageName, 0)

                                            // Get the launch intent
                                            val simToolkitIntent = packageManager.getLaunchIntentForPackage(packageName)
                                            if (simToolkitIntent != null) {
                                                simToolkitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                navController.context.startActivity(simToolkitIntent)
                                                println("BookingConfirmationScreen: Successfully launched SIM Toolkit app: $packageName")
                                                snackbarHostState.showSnackbar("Opening SIM Toolkit for payment")
                                                launchSuccess = true
                                                break
                                            }
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            println("BookingConfirmationScreen: Package $packageName not found")
                                            // Continue to next package in the list
                                        } catch (e: Exception) {
                                            println("BookingConfirmationScreen: Failed to launch $packageName: ${e.message}")
                                            // Continue to next package in the list
                                        }
                                    }

                                    // If no SIM Toolkit app could be launched
                                    if (!launchSuccess) {
                                        println("BookingConfirmationScreen: All SIM Toolkit launch attempts failed")
                                        snackbarHostState.showSnackbar(
                                            message = "Couldn't open SIM Toolkit. Please dial *100# manually for M-Pesa services",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Pay via SIM Toolkit", fontWeight = FontWeight.Bold)
                        }

                        // Alternative payment instructions
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Payment Instructions:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1. Dial *100# on your phone",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "2. Select M-Pesa Services",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "3. Select Lipa na M-Pesa > Pay Bill",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "4. Enter Business Number: 123456",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "5. Enter Amount: ${currencyFormat.format(eventData.price)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "6. Enter PIN and confirm payment",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}