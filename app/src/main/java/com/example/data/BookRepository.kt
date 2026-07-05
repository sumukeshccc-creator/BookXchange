package com.example.data

import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    private val userDao: UserDao,
    private val messageDao: MessageDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    fun getBookById(id: Int): Flow<Book?> = bookDao.getBookById(id)

    suspend fun insertBook(book: Book) {
        bookDao.insertBook(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }
    
    // User functions
    fun getUser(username: String): Flow<User?> = userDao.getUser(username)
    
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    // Message functions
    fun getMessagesForBook(bookId: Int): Flow<List<Message>> = messageDao.getMessagesForBook(bookId)
    
    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(message)
    }
}
