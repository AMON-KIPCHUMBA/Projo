package com.example.eventflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.eventflow.navigation.NavGraph
import com.example.eventflow.ui.theme.EventFlowTheme
import com.example.eventflow.viewmodel.AuthViewModel
import com.example.eventflow.viewmodel.EventViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authViewModel = AuthViewModel()
    private val eventViewModel = EventViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventFlowTheme {
                EventFlowApp(authViewModel, eventViewModel)
            }
        }
    }
}

@Composable
fun EventFlowApp(authViewModel: AuthViewModel, eventViewModel: EventViewModel) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    // Listen to auth state changes
    LaunchedEffect(Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // Clear back stack and navigate to login only on explicit logout or session expiration
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                // Ensure user stays on current screen or navigates to dashboard on app start
                if (navController.currentDestination?.route == "login") {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }

    NavGraph(
        navController = navController,
        authViewModel = authViewModel,
        eventViewModel = eventViewModel
    )
}