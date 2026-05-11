package com.example.jjhg.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE customerId = :customerId")
    fun getNetBalance(customerId: Int): Flow<Double?>

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT transactions.*, customers.name as customerName FROM transactions INNER JOIN customers ON transactions.customerId = customers.id")
    fun getAllTransactionsWithCustomerName(): Flow<List<TransactionWithCustomer>>

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Int)
}

data class TransactionWithCustomer(
    @Embedded val transaction: Transaction,
    val customerName: String
)
