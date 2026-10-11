package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.TaskRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.TaskFlowTheme
import com.example.ui.viewmodel.TaskViewModel
import com.example.ui.viewmodel.TaskViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaskFlowApp()
        }
    }
}

@Composable
fun TaskFlowApp() {
    val context = LocalContext.current
    val auth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        TaskFlowTheme(darkTheme = isSystemInDarkTheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    }
                )
            }
        }
    } else {
        val repository = remember(context) { TaskRepository(context) }
        val viewModel: TaskViewModel = viewModel(
            factory = TaskViewModelFactory(repository, context.applicationContext)
        )
        val uiState by viewModel.uiState.collectAsState()

        val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()

        TaskFlowTheme(darkTheme = isDark) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MainAppScreen(
                    viewModel = viewModel,
                    onSignOutRequested = {
                        currentUser = null
                    }
                )
            }
        }
    }
}
