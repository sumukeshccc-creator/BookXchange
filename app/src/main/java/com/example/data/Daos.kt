package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY timestamp DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Int): Flow<Book?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Int)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUser(username: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getMessagesForBook(bookId: Int): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)
}
