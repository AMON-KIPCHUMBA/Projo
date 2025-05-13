package com.example.eventflow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eventflow.screens.BookingConfirmationScreen
import com.example.eventflow.screens.EventDetailsScreen
import com.example.eventflow.screens.WishlistScreen
import com.example.eventflow.screens.auth.LoginScreen
import com.example.eventflow.screens.auth.SignUpScreen
import com.example.eventflow.screens.dashboard.DashboardScreen
import com.example.eventflow.screens.event.AddEditEventScreen
import com.example.eventflow.screens.event.EventListScreen
import com.example.eventflow.screens.profile.ProfileScreen
import com.example.eventflow.viewmodel.AuthViewModel
import com.example.eventflow.viewmodel.EventViewModel

@Composable
fun NavGraph(navController: NavHostController, authViewModel: AuthViewModel, eventViewModel: EventViewModel) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_LOGIN
    ) {
        composable(ROUTE_LOGIN) {
            LoginScreen(navController, authViewModel)
        }
        composable(ROUTE_SIGNUP) {
            SignUpScreen(navController, authViewModel)
        }
        composable(ROUTE_DASHBOARD) {
            DashboardScreen(navController, authViewModel, eventViewModel)
        }
        composable(ROUTE_EVENT_LIST) {
            EventListScreen(navController, eventViewModel)
        }
        composable(
            route = "$ROUTE_ADD_EDIT_EVENT?eventId={eventId}",
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val eventIdArg = backStackEntry.arguments?.getString("eventId")
            println("NavGraph: Navigating to AddEditEventScreen with eventIdArg=$eventIdArg")
            AddEditEventScreen(navController, eventViewModel, eventIdArg)
        }
        composable(ROUTE_PROFILE) {
            ProfileScreen(navController, authViewModel)
        }
        composable(ROUTE_WISHLIST) {
            WishlistScreen(navController)
        }
        composable(
            route = ROUTE_EVENT_DETAILS,
            arguments = listOf(navArgument("eventId") { defaultValue = "" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            println("NavGraph: Navigating to EventDetailsScreen with eventId=$eventId")
            EventDetailsScreen(navController, eventId, eventViewModel)
        }
        composable(
            route = ROUTE_BOOKING_CONFIRMATION,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            println("NavGraph: Navigating to BookingConfirmationScreen with eventId=$eventId")
            BookingConfirmationScreen(navController, eventId, eventViewModel)
        }
    }
}