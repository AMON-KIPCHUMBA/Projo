package com.example.eventflow.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.eventflow.data.AuthRepository
import com.example.eventflow.models.EventModel
import com.example.eventflow.navigation.*
import com.example.eventflow.viewmodel.AuthViewModel
import com.example.eventflow.viewmodel.EventViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    val authRepository = AuthRepository()
    val userId = authRepository.getCurrentUserId() ?: return
    val events = eventViewModel.events.collectAsState().value
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch events
    LaunchedEffect(Unit) {
        eventViewModel.fetchEvents(userId)
    }

    // Bottom navigation items
    val navItems = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, ROUTE_DASHBOARD),
        NavItem("Events", Icons.Filled.List, Icons.Outlined.List, ROUTE_EVENT_LIST),
        NavItem("Wishlist", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, ROUTE_WISHLIST),
        NavItem("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, ROUTE_PROFILE)
    )

    // Current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // State for refreshing animation
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EventFlow",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                eventViewModel.fetchEvents(userId)
                                delay(800)
                                isRefreshing = false
                                snackbarHostState.showSnackbar("Events refreshed")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isRefreshing) Icons.Filled.Refresh else Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route) item.selectedIcon else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(ROUTE_ADD_EDIT_EVENT) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to EventFlow",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Manage your events efficiently",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            progress = if (events.isEmpty()) 0f else minOf(events.size / 10f, 1f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${events.size} event(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = getClosestEventDate(events),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Upcoming Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { navController.navigate(ROUTE_EVENT_LIST) }
                    ) {
                        Text("View All")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = events.isEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "No upcoming events",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Tap + to add a new event",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = events.isNotEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    LazyColumn {
                        itemsIndexed(
                            items = events.sortedBy { parseDate(it.date) }.take(5),
                            key = { _, event -> event.eventId }
                        ) { index, event ->
                            EventCard(
                                event = event,
                                index = index,
                                onClick = { navController.navigate("event_details/${event.eventId}") },
                                onWishlistClick = {
                                    coroutineScope.launch {
                                        val result = authRepository.addToWishlist(userId, event)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("Added to wishlist")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to add to wishlist")
                                        }
                                    }
                                }
                            )

                            if (index < minOf(events.size - 1, 4)) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        if (events.size > 5) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = { navController.navigate(ROUTE_EVENT_LIST) },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text("View ${events.size - 5} More Events")
                                }

                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }

            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: EventModel,
    index: Int,
    onClick: () -> Unit,
    onWishlistClick: () -> Unit
) {
    val cardColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface
    )

    val textColors = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.onSurface
    )

    val colorIndex = index % cardColors.size
    val isUpcoming = isEventUpcoming(event.date)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColors[colorIndex]
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = getEventDay(event.date),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColors[colorIndex]
                    )
                    Text(
                        text = getEventMonth(event.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColors[colorIndex]
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColors[colorIndex],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    IconButton(onClick = onWishlistClick) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Add to Wishlist",
                            tint = textColors[colorIndex]
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textColors[colorIndex].copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${event.date} at ${event.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColors[colorIndex].copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textColors[colorIndex].copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColors[colorIndex].copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = textColors[colorIndex].copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper functions for date formatting and comparison
fun getEventDay(dateString: String): String {
    return try {
        val parts = dateString.split("/")
        if (parts.size >= 2) parts[1] else "--"
    } catch (e: Exception) {
        "--"
    }
}

fun getEventMonth(dateString: String): String {
    return try {
        val parts = dateString.split("/")
        if (parts.size >= 2) {
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIndex = parts[0].toInt() - 1
            if (monthIndex in 0..11) monthNames[monthIndex] else "--"
        } else "--"
    } catch (e: Exception) {
        "--"
    }
}

fun parseDate(dateString: String): Date {
    return try {
        val format = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        format.parse(dateString) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

fun isEventUpcoming(dateString: String): Boolean {
    val eventDate = parseDate(dateString)
    val currentDate = Calendar.getInstance().time
    val threeDaysFromNow = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 3)
    }.time

    return eventDate.after(currentDate) && eventDate.before(threeDaysFromNow)
}

fun getClosestEventDate(events: List<EventModel>): String {
    if (events.isEmpty()) return "No events"

    val currentDate = Calendar.getInstance().time

    val upcomingEvents = events
        .map { parseDate(it.date) }
        .filter { it.after(currentDate) }
        .sorted()

    if (upcomingEvents.isEmpty()) return "No upcoming events"

    val closestDate = upcomingEvents.first()
    val diff = (closestDate.time - currentDate.time) / (1000 * 60 * 60 * 24)

    return when {
        diff == 0L -> "Today"
        diff == 1L -> "Tomorrow"
        diff < 7 -> "In $diff days"
        else -> "In ${diff / 7} weeks"
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val route: String
)