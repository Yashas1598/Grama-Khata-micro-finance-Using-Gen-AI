package com.example.jjhg.data

import kotlinx.coroutines.flow.Flow

class GramaKhataRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun insertCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        transactionDao.deleteTransactionsForCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun getCustomerById(id: Int): Customer? {
        return customerDao.getCustomerById(id)
    }

    fun getTransactionsForCustomer(customerId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForCustomer(customerId)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    fun getNetBalance(customerId: Int): Flow<Double?> {
        return transactionDao.getNetBalance(customerId)
    }

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    fun getAllTransactionsWithCustomer(): Flow<List<TransactionWithCustomer>> {
        return transactionDao.getAllTransactionsWithCustomerName()
    }
}
