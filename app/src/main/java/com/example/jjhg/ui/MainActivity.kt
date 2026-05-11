package com.example.jjhg.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.jjhg.R
import com.example.jjhg.data.AppDatabase
import com.example.jjhg.data.Customer
import com.example.jjhg.data.GramaKhataRepository
import com.example.jjhg.databinding.ActivityMainBinding
import com.example.jjhg.databinding.DialogAddCustomerBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GramaKhataRepository(database.customerDao(), database.transactionDao())
        MainViewModelFactory(repository)
    }

    private var selectedPhotoUri: Uri? = null
    private var currentDialogBinding: DialogAddCustomerBinding? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedPhotoUri = it
            currentDialogBinding?.imageViewSelectedPhoto?.load(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = CustomerAdapter(
            onClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java)
                intent.putExtra("CUSTOMER_ID", customer.id)
                intent.putExtra("CUSTOMER_NAME", customer.name)
                startActivity(intent)
            },
            onDelete = { customer ->
                showDeleteConfirmation(customer)
            }
        )
        binding.recyclerViewCustomers.adapter = adapter

        viewModel.customersWithBalance.observe(this) { customers ->
            adapter.submitList(customers)
        }

        viewModel.totalPendingCollection.observe(this) { giveTotal ->
            binding.textViewTotalGive.text = "₹${String.format(java.util.Locale.getDefault(), "%.2f", giveTotal ?: 0.0)}"
        }

        viewModel.totalYouWillTake.observe(this) { takeTotal ->
            binding.textViewTotalTake.text = "₹${String.format(java.util.Locale.getDefault(), "%.2f", takeTotal ?: 0.0)}"
        }

        binding.fabAddCustomer.setOnClickListener {
            showAddCustomerDialog()
        }

        binding.editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun showAddCustomerDialog() {
        val dialogBinding = DialogAddCustomerBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding
        selectedPhotoUri = null

        dialogBinding.btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Customer")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.editTextName.text.toString().trim()
                val phone = dialogBinding.editTextPhone.text.toString().trim()
                
                if (name.isNotBlank()) {
                    viewModel.insertCustomer(name, phone, selectedPhotoUri?.toString())
                    Toast.makeText(this, "Customer added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                currentDialogBinding = null
            }
            .setOnDismissListener {
                currentDialogBinding = null
            }
            .show()
    }

    private fun showDeleteConfirmation(customer: Customer) {
        AlertDialog.Builder(this)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete ${customer.name}? This will remove all their transaction history.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCustomer(customer)
                Toast.makeText(this, "${customer.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_report -> {
                viewModel.generateDailyReport { report ->
                    showReportDialog(report)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showReportDialog(report: String) {
        AlertDialog.Builder(this)
            .setTitle("Daily Collection Report")
            .setMessage(report)
            .setPositiveButton("Share") { _, _ ->
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, report)
                startActivity(Intent.createChooser(shareIntent, "Share Report"))
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
