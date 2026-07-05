package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BookRepository
import com.example.ui.BookViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "book_database"
        ).fallbackToDestructiveMigration().build()
        
        val repository = BookRepository(database.bookDao(), database.userDao(), database.messageDao())

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val viewModelFactory = BookViewModel.Factory(repository, context)
                val viewModel: BookViewModel = viewModel(factory = viewModelFactory)
                BookBorrowApp(viewModel)
            }
        }
    }
}

@Composable
fun BookBorrowApp(viewModel: BookViewModel) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "main" else "login"
    ) {
        composable("login") {
            // Auto redirect if already logged in
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            LoginScreen(
                onLoginSuccess = { username ->
                    viewModel.login(username)
                }
            )
        }
        
        composable("main") {
            // Auto redirect to login if logged out
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }
            
            if (currentUser != null) {
                MainScreen(viewModel, navController)
            }
        }
        
        composable(
            "chat/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId") ?: return@composable
            ChatScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
