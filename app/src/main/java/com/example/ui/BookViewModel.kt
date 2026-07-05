package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BookViewModel(
    private val repository: BookRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _currentUser = MutableStateFlow<String?>(sharedPrefs.getString("auth_token", null))
    val currentUser = _currentUser.asStateFlow()

    val allBooks: StateFlow<List<Book>> = repository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow("ALL")
    val filterStatus = _filterStatus.asStateFlow()

    val filteredBooks: StateFlow<List<Book>> = combine(
        allBooks,
        _searchQuery,
        _filterStatus
    ) { books, query, status ->
        books.filter { book ->
            val matchesSearch = book.title.contains(query, ignoreCase = true) || 
                                book.author.contains(query, ignoreCase = true)
            val matchesStatus = if (status == "ALL") true else book.status == status
            matchesSearch && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate test books if empty
        viewModelScope.launch {
            repository.allBooks.firstOrNull()?.let { books ->
                if (books.isEmpty()) {
                    addTestBooks()
                }
            }
        }
    }

    private fun addTestBooks() {
        val testBooks = listOf(
            Book(title = "The Great Gatsby", author = "F. Scott Fitzgerald", condition = "Good", ownerName = "Alice"),
            Book(title = "1984", author = "George Orwell", condition = "Mint", ownerName = "Bob"),
            Book(title = "To Kill a Mockingbird", author = "Harper Lee", condition = "Fair", ownerName = "Charlie"),
            Book(title = "Pride and Prejudice", author = "Jane Austen", condition = "Good", ownerName = "Alice"),
            Book(title = "The Catcher in the Rye", author = "J.D. Salinger", condition = "Poor", ownerName = "Bob")
        )
        viewModelScope.launch {
            testBooks.forEach { repository.insertBook(it) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun login(username: String) {
        if (username.isNotBlank()) {
            sharedPrefs.edit().putString("auth_token", username).apply()
            _currentUser.value = username
            
            viewModelScope.launch {
                repository.insertUser(User(username = username))
            }
        }
    }

    fun logout() {
        sharedPrefs.edit().remove("auth_token").apply()
        _currentUser.value = null
    }

    fun getUser(username: String): Flow<User?> = repository.getUser(username)

    fun addBook(title: String, author: String, condition: String) {
        val owner = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertBook(
                Book(
                    title = title,
                    author = author,
                    condition = condition,
                    ownerName = owner
                )
            )
        }
    }

    fun requestBook(book: Book) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateBook(book.copy(
                status = "REQUESTED",
                requestedByName = user
            ))
        }
    }

    fun acceptRequest(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book.copy(
                status = "PENDING_TRANSFER"
            ))
        }
    }
    
    fun declineRequest(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book.copy(
                status = "AVAILABLE",
                requestedByName = null
            ))
        }
    }

    fun transferBookInitiated(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book.copy(
                status = "PENDING_RECEIPT"
            ))
        }
    }

    fun acceptTransfer(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book.copy(
                status = "BORROWED",
                borrowerName = book.requestedByName,
                isAvailable = false
            ))
        }
    }

    fun returnBook(book: Book, returnedCondition: String) {
        viewModelScope.launch {
            val isSameCondition = book.condition == returnedCondition
            
            repository.updateBook(book.copy(
                status = "AVAILABLE",
                borrowerName = null,
                requestedByName = null,
                isAvailable = true,
                condition = returnedCondition
            ))
            
            book.borrowerName?.let { borrowerName ->
                repository.getUser(borrowerName).firstOrNull()?.let { borrower ->
                    val newScore = if (isSameCondition) borrower.trustScore + 10 else borrower.trustScore - 20
                    repository.updateUser(borrower.copy(
                        trustScore = newScore,
                        completedSwaps = borrower.completedSwaps + 1
                    ))
                }
            }
            
            repository.getUser(book.ownerName).firstOrNull()?.let { owner ->
                repository.updateUser(owner.copy(
                    trustScore = owner.trustScore + 5,
                    completedSwaps = owner.completedSwaps + 1
                ))
            }
        }
    }

    // Firestore Real-time Chat Implementation
    fun getMessages(bookId: Int): Flow<List<Message>> = callbackFlow {
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null // Handle case where Firebase isn't initialized
        }
        
        if (db == null) {
            // Fallback to local Room DB if Firebase is not configured
            val localFlow = repository.getMessagesForBook(bookId)
            val job = launch {
                localFlow.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listener = db.collection("chats")
            .document(bookId.toString())
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    fun sendMessage(bookId: Int, receiver: String, content: String) {
        val sender = _currentUser.value ?: return
        val message = Message(
            bookId = bookId,
            sender = sender,
            receiver = receiver,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("chats").document(bookId.toString()).collection("messages").add(message)
        } catch (e: Exception) {
            // Fallback to local Room DB
            viewModelScope.launch {
                repository.insertMessage(message)
            }
        }
    }

    class Factory(
        private val repository: BookRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
                val prefs = context.getSharedPreferences("book_borrow_prefs", Context.MODE_PRIVATE)
                return BookViewModel(repository, prefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
