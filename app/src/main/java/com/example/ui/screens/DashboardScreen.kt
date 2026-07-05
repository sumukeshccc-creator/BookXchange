package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Book
import androidx.compose.ui.platform.testTag
import com.example.ui.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    books: List<Book>, // Passed from MainScreen but we can use the state directly
    currentUser: String,
    viewModel: BookViewModel,
    onAddBookClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onChatClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val userFlow = viewModel.getUser(currentUser).collectAsState(initial = null)
    val filteredBooks by viewModel.filteredBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Library Feed", fontWeight = FontWeight.ExtraBold)
                        userFlow.value?.let { user ->
                            Text(
                                "Trust Score: ${user.trustScore} • Swaps: ${user.completedSwaps}", 
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onLogoutClick, modifier = Modifier.testTag("logout_button")) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBookClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_book_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Book")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Filter Bar
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by title or author...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statuses = listOf("ALL", "AVAILABLE", "REQUESTED", "PENDING_TRANSFER", "PENDING_RECEIPT", "BORROWED")
                    items(statuses) { status ->
                        FilterChip(
                            selected = filterStatus == status,
                            onClick = { viewModel.updateFilterStatus(status) },
                            label = { 
                                val label = if (status == "ALL") "All Books" else status.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
                                Text(label) 
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider()

            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No books found.\nTry adjusting your search or add a book!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        BookCard(
                            book = book, 
                            isOwner = book.ownerName == currentUser,
                            currentUser = currentUser,
                            viewModel = viewModel,
                            onChatClick = { onChatClick(book.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: Book, 
    isOwner: Boolean, 
    currentUser: String,
    viewModel: BookViewModel,
    onChatClick: () -> Unit
) {
    var showReturnDialog by remember { mutableStateOf(false) }
    var showTransferScanDialog by remember { mutableStateOf(false) }
    
    if (showReturnDialog) {
        var scannedCondition by remember { mutableStateOf(book.condition) }
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("AI Condition Verification") },
            text = { 
                Column {
                    Text("Scanning the returned book... (Simulated)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detected condition:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mint", "Good", "Fair", "Poor").forEach { cond ->
                            FilterChip(
                                selected = scannedCondition == cond,
                                onClick = { scannedCondition = cond },
                                label = { Text(cond) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (scannedCondition == book.condition) {
                        Text("Perfect! Condition matches. +10 Trust Points for Borrower.", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Condition degraded! -20 Trust Points for Borrower.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.returnBook(book, scannedCondition)
                    showReturnDialog = false
                }) {
                    Text("Complete Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTransferScanDialog) {
        AlertDialog(
            onDismissRequest = { showTransferScanDialog = false },
            title = { Text("Scan to Handover") },
            text = { Text("Simulating scanning the book to prove handover to ${book.requestedByName}...") },
            confirmButton = {
                Button(onClick = {
                    viewModel.transferBookInitiated(book)
                    showTransferScanDialog = false
                }) {
                    Text("Scan Successful")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferScanDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("book_card_${book.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val badgeColor = when(book.status) {
                    "AVAILABLE" -> MaterialTheme.colorScheme.primary
                    "REQUESTED" -> MaterialTheme.colorScheme.tertiary
                    "PENDING_TRANSFER", "PENDING_RECEIPT" -> MaterialTheme.colorScheme.secondary
                    "BORROWED" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                val statusText = if (book.status == "PENDING_RECEIPT") "Transferring" else book.status
                
                Badge(
                    containerColor = badgeColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "by ${book.author}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Condition: ${book.condition}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = if (isOwner) "You own this" else "Shared by ${book.ownerName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Interaction logic based on ownership and status
            if (isOwner) {
                when (book.status) {
                    "AVAILABLE" -> {
                        Text("Waiting for requests...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "REQUESTED" -> {
                        val requesterFlow = book.requestedByName?.let { viewModel.getUser(it).collectAsState(initial = null) }
                        Column {
                            Text("Requested by ${book.requestedByName}", fontWeight = FontWeight.Bold)
                            requesterFlow?.value?.let { requester ->
                                Text("Trust Score: ${requester.trustScore} (Swaps: ${requester.completedSwaps})", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.acceptRequest(book) }, modifier = Modifier.weight(1f)) {
                                    Text("Accept")
                                }
                                OutlinedButton(onClick = { viewModel.declineRequest(book) }, modifier = Modifier.weight(1f)) {
                                    Text("Decline")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Chat with Requester")
                            }
                        }
                    }
                    "PENDING_TRANSFER" -> {
                        Column {
                            Text("Meet up with ${book.requestedByName} to hand over the book.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showTransferScanDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Scan to Handover")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Chat with ${book.requestedByName}")
                            }
                        }
                    }
                    "PENDING_RECEIPT" -> {
                        Column {
                            Text("Waiting for ${book.requestedByName} to confirm receipt on their phone.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Pending Confirmation")
                            }
                        }
                    }
                    "BORROWED" -> {
                        Column {
                            Text("Borrowed by ${book.borrowerName}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showReturnDialog = true }, 
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Verify Return (AI Scan)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Chat with ${book.borrowerName}")
                            }
                        }
                    }
                }
            } else {
                // Not the owner
                when (book.status) {
                    "AVAILABLE" -> {
                        Button(
                            onClick = { viewModel.requestBook(book) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Request to Borrow")
                        }
                    }
                    "REQUESTED" -> {
                        if (book.requestedByName == currentUser) {
                            Column {
                                OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                    Text("Request Pending Approval")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                    Text("Chat with Owner")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Requested by someone else")
                            }
                        }
                    }
                    "PENDING_TRANSFER" -> {
                        if (book.requestedByName == currentUser) {
                            Column {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Request Accepted! Chat to arrange meetup. Owner will scan to transfer.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                    Text("Chat with Owner")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Reserved")
                            }
                        }
                    }
                    "PENDING_RECEIPT" -> {
                        if (book.requestedByName == currentUser) {
                            Column {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Owner has scanned the book. Please confirm receipt.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.acceptTransfer(book) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Accept Book")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Transferring")
                            }
                        }
                    }
                    "BORROWED" -> {
                        if (book.borrowerName == currentUser) {
                            Column {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "You are currently borrowing this. Meet owner to return and scan condition.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                    Text("Chat with Owner")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Currently Borrowed")
                            }
                        }
                    }
                }
            }
        }
    }
}
