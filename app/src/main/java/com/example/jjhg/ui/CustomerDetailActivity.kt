package com.example.jjhg.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.jjhg.data.AppDatabase
import com.example.jjhg.data.GramaKhataRepository
import com.example.jjhg.databinding.ActivityCustomerDetailBinding

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GramaKhataRepository(database.customerDao(), database.transactionDao())
        MainViewModelFactory(repository)
    }

    private var customerId: Int = -1
    private var customerName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getIntExtra("CUSTOMER_ID", -1)
        customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""

        binding.textViewCustomerName.text = customerName

        val adapter = TransactionAdapter()
        binding.recyclerViewTransactions.adapter = adapter

        viewModel.getTransactions(customerId).observe(this) { transactions ->
            adapter.submitList(transactions)
        }

        viewModel.getNetBalance(customerId).observe(this) { balance ->
            val currentBalance = balance ?: 0.0
            binding.textViewBalance.text = "Balance: ₹${String.format("%.2f", currentBalance)}"
            
            binding.btnRemind.setOnClickListener {
                if (currentBalance > 0) {
                    sendReminder(currentBalance)
                }
            }
        }

        binding.btnGive.setOnClickListener { showTransactionDialog(true) }
        binding.btnTake.setOnClickListener { showTransactionDialog(false) }
    }

    private fun sendReminder(amount: Double) {
        val message = "Namaskara, your due at Grama-Khata Shop is ₹${String.format("%.2f", amount)}."
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, message)
        // Try to target WhatsApp if possible, or just show chooser
        startActivity(Intent.createChooser(intent, "Send Reminder via"))
    }

    private fun showTransactionDialog(isGive: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (isGive) "Give (Credit)" else "Take (Payment)")
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount"
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val amountStr = input.text.toString()
            if (amountStr.isNotBlank()) {
                val amount = amountStr.toDouble()
                viewModel.addTransaction(customerId, if (isGive) amount else -amount)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
