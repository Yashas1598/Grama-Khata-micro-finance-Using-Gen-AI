package com.example.jjhg.ui

import androidx.lifecycle.*
import com.example.jjhg.data.Customer
import com.example.jjhg.data.CustomerWithBalance
import com.example.jjhg.data.GramaKhataRepository
import com.example.jjhg.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class MainViewModel(private val repository: GramaKhataRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val customersWithBalance: LiveData<List<CustomerWithBalance>> = 
        combine(
            repository.allCustomers, 
            repository.getAllTransactions(), 
            searchQuery
        ) { customers, transactions, query ->
            val trimmedQuery = query.trim()
            customers
                .filter { it.name.contains(trimmedQuery, ignoreCase = true) }
                .map { customer ->
                    val balance = transactions.filter { it.customerId == customer.id }.sumOf { it.amount }
                    CustomerWithBalance(customer, balance)
                }.sortedByDescending { Math.abs(it.balance) }
        }.asLiveData()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    val totalPendingCollection: LiveData<Double> = customersWithBalance.map { list ->
        list.filter { it.balance > 0 }.sumOf { it.balance }
    }

    val totalYouWillTake: LiveData<Double> = customersWithBalance.map { list ->
        list.filter { it.balance < 0 }.sumOf { -it.balance }
    }

    fun insertCustomer(name: String, phoneNumber: String? = null, photoUri: String? = null) {
        viewModelScope.launch {
            repository.insertCustomer(Customer(name = name, phoneNumber = phoneNumber, photoUri = photoUri))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun addTransaction(customerId: Int, amount: Double, description: String? = null) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(customerId = customerId, amount = amount, description = description)
            )
        }
    }

    fun getTransactions(customerId: Int): LiveData<List<Transaction>> {
        return repository.getTransactionsForCustomer(customerId).asLiveData()
    }

    fun getNetBalance(customerId: Int): LiveData<Double?> {
        return repository.getNetBalance(customerId).asLiveData()
    }

    fun generateDailyReport(onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.getAllTransactionsWithCustomer().collect { transactionsWithCustomer ->
                val today = java.util.Calendar.getInstance()
                today.set(java.util.Calendar.HOUR_OF_DAY, 0)
                today.set(java.util.Calendar.MINUTE, 0)
                today.set(java.util.Calendar.SECOND, 0)
                today.set(java.util.Calendar.MILLISECOND, 0)
                val startOfDay = today.timeInMillis

                val todayTransactions = transactionsWithCustomer.filter { it.transaction.timestamp >= startOfDay }
                val totalCollected = todayTransactions.filter { it.transaction.amount < 0 }.sumOf { -it.transaction.amount }
                val totalCredit = todayTransactions.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount }

                val report = StringBuilder()
                report.append("Daily Collection Report\n")
                report.append("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
                report.append("Total Collected (Cash): ₹${String.format("%.2f", totalCollected)}\n")
                report.append("Total Given (Credit): ₹${String.format("%.2f", totalCredit)}\n")
                report.append("\nTransactions:\n")
                
                todayTransactions.forEach { t ->
                    val type = if (t.transaction.amount > 0) "CREDIT" else "CASH"
                    report.append("${t.customerName} - $type: ₹${String.format("%.2f", Math.abs(t.transaction.amount))}\n")
                }

                onResult(report.toString())
            }
        }
    }
}

class MainViewModelFactory(private val repository: GramaKhataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
