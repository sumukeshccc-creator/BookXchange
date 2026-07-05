package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val condition: String, // Mint, Good, Fair, Poor
    val ownerName: String,
    val isAvailable: Boolean = true,
    val borrowerName: String? = null,
    val requestedByName: String? = null,
    val status: String = "AVAILABLE", // AVAILABLE, REQUESTED, PENDING_TRANSFER, BORROWED, PENDING_RETURN
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val trustScore: Int = 100,
    val completedSwaps: Int = 0
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val sender: String,
    val receiver: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
