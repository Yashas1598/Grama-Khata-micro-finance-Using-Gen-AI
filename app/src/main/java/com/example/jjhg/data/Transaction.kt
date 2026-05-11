package com.example.jjhg.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double, // Positive for Give (Credit), Negative for Take (Payment)
    val timestamp: Long = System.currentTimeMillis(),
    val description: String? = null
)
