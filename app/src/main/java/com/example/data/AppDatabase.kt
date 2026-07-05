package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Book::class, User::class, Message::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}
