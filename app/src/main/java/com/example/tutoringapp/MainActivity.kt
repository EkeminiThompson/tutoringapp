package com.example.tutoringapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tutoringapp.ui.theme.TutoringappTheme
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TutoringappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TutoringAppNavHost()
                }
            }
        }
    }
}

@Composable
fun TutoringAppNavHost() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    NavHost(
        navController = navController,
        startDestination = if (auth.currentUser == null) "signup" else "dashboard"
    ) {
        composable("signup") {
            SignupScreen(
                onNavigateToTeacherProfile = { navController.navigate("teacher_profile") },
                onNavigateToDashboard = { navController.navigate("dashboard") }
            )
        }
        composable("teacher_profile") {
            TeacherProfileScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToProfile = { userId -> navController.navigate("profile/$userId") }
            )
        }
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateToChat = { teacherId -> navController.navigate("chat/$teacherId") }
            )
        }
        composable("chat/{teacherId}") { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            ChatScreen(teacherId = teacherId)
        }
    }
}