package com.example.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.BookViewModel

@Composable
fun MainScreen(viewModel: BookViewModel, rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val allBooks by viewModel.allBooks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        if (currentRoute != "dashboard") {
                            bottomNavController.navigate("dashboard") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                
                NavigationBarItem(
                    selected = currentRoute == "map",
                    onClick = {
                        if (currentRoute != "map") {
                            bottomNavController.navigate("map") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                
                NavigationBarItem(
                    selected = currentRoute == "camera",
                    onClick = {
                        if (currentRoute != "camera") {
                            bottomNavController.navigate("camera") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                    label = { Text("Scan") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    books = allBooks,
                    currentUser = currentUser ?: "",
                    viewModel = viewModel,
                    onAddBookClick = { bottomNavController.navigate("camera") },
                    onLogoutClick = { viewModel.logout() },
                    onChatClick = { bookId -> rootNavController.navigate("chat/$bookId") }
                )
            }
            
            composable("map") {
                MapScreen(books = allBooks)
            }
            
            composable("camera") {
                CameraScreen(
                    onBookScanned = { title, author, condition ->
                        viewModel.addBook(title, author, condition)
                        bottomNavController.navigate("dashboard") {
                            popUpTo("dashboard")
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
