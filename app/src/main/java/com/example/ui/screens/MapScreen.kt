package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Book
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(books: List<Book>, modifier: Modifier = Modifier) {
    // Hulimavu, Bengaluru
    val fallbackLocation = LatLng(12.8770, 77.5993)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallbackLocation, 14f)
    }

    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Mocking pins around Hulimavu
            books.forEachIndexed { index, book ->
                val offsetLat = (index % 5 - 2) * 0.005
                val offsetLng = (index / 5) * 0.005
                Marker(
                    state = MarkerState(position = LatLng(fallbackLocation.latitude + offsetLat, fallbackLocation.longitude + offsetLng)),
                    title = book.title,
                    snippet = "Condition: ${book.condition}",
                    onClick = {
                        selectedBook = book
                        showSheet = true
                        true // Consume the click
                    }
                )
            }
        }
    }

    if (showSheet && selectedBook != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedBook!!.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "by ${selectedBook!!.author}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Condition: ${selectedBook!!.condition}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        showSheet = false 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Request to Borrow", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
