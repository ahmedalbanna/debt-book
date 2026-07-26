package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CustomerEntity
import com.example.data.CustomerWithBalance
import com.example.data.LedgerRepository
import com.example.data.ParsedVoiceInput
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.speech.SpeechRecognizerManager
import com.example.speech.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VoiceDialogUiState(
    val isOpen: Boolean = false,
    val rawText: String = "",
    val parsedInput: ParsedVoiceInput? = null,
    val editedCustomerName: String = "",
    val editedAmount: String = "",
    val editedType: TransactionType = TransactionType.DEBT,
    val editedNotes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = LedgerRepository(db.customerDao(), db.transactionDao())
    val speechManager = SpeechRecognizerManager(application)

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = repository.customersWithBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDebts: StateFlow<Double> = repository.totalDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double> = repository.totalPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query for customers
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
        customersWithBalance,
        searchQuery
    ) { customers, query ->
        if (query.isBlank()) customers
        else customers.filter {
            it.customer.name.contains(query, ignoreCase = true) ||
                    it.customer.phone.contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected customer for detail screen
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    val selectedCustomerWithBalance: StateFlow<CustomerWithBalance?> = combine(
        customersWithBalance,
        selectedCustomerId
    ) { list, id ->
        list.find { it.customer.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedCustomerTransactions: StateFlow<List<TransactionEntity>> = selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) repository.getTransactionsForCustomer(id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Dialog State
    private val _voiceState = MutableStateFlow(VoiceDialogUiState())
    val voiceState: StateFlow<VoiceDialogUiState> = _voiceState.asStateFlow()

    fun openVoiceDialog() {
        speechManager.resetState()
        _voiceState.value = VoiceDialogUiState(isOpen = true)
    }

    fun closeVoiceDialog() {
        speechManager.stopListening()
        speechManager.resetState()
        _voiceState.value = VoiceDialogUiState(isOpen = false)
    }

    fun startVoiceListening() {
        speechManager.startListening()
    }

    fun stopVoiceListening() {
        speechManager.stopListening()
    }

    fun onVoiceResultReceived(recognizedText: String) {
        viewModelScope.launch {
            val parsed = repository.parseVoiceCommand(recognizedText)
            _voiceState.value = _voiceState.value.copy(
                rawText = recognizedText,
                parsedInput = parsed,
                editedCustomerName = parsed.detectedCustomerName,
                editedAmount = if (parsed.amount > 0) parsed.amount.toInt().toString() else "",
                editedType = parsed.transactionType,
                editedNotes = parsed.notes
            )
        }
    }

    fun simulateVoicePreset(phrase: String) {
        speechManager.simulateVoiceInput(phrase)
        onVoiceResultReceived(phrase)
    }

    fun updateVoiceFields(
        customerName: String? = null,
        amount: String? = null,
        type: TransactionType? = null,
        notes: String? = null
    ) {
        _voiceState.value = _voiceState.value.copy(
            editedCustomerName = customerName ?: _voiceState.value.editedCustomerName,
            editedAmount = amount ?: _voiceState.value.editedAmount,
            editedType = type ?: _voiceState.value.editedType,
            editedNotes = notes ?: _voiceState.value.editedNotes
        )
    }

    fun confirmAndSaveVoiceTransaction() {
        val state = _voiceState.value
        val name = state.editedCustomerName.trim()
        val amount = state.editedAmount.toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            _voiceState.value = state.copy(error = "يرجى كتابة أو التعرّف على اسم الزبون")
            return
        }
        if (amount <= 0.0) {
            _voiceState.value = state.copy(error = "يرجى تحديد مبلغ صحيح أكبر من الصفر")
            return
        }

        viewModelScope.launch {
            _voiceState.value = state.copy(isSaving = true, error = null)
            val customer = repository.getOrCreateCustomerByName(name)
            repository.addTransaction(
                customerId = customer.id,
                type = state.editedType,
                amount = amount,
                description = state.editedNotes,
                rawVoiceText = state.rawText.ifBlank { null }
            )
            closeVoiceDialog()
        }
    }

    // Manual Quick Add Transaction (from customer detail screen)
    fun addManualTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                customerId = customerId,
                type = type,
                amount = amount,
                description = description
            )
        }
    }

    // Add Customer manually
    fun createCustomer(name: String, phone: String, notes: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addCustomer(name, phone, notes)
            }
        }
    }

    // Delete customer
    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomerId.value == customer.id) {
                _selectedCustomerId.value = null
            }
        }
    }

    // Delete single transaction
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun generateWhatsAppStatementText(
        item: CustomerWithBalance,
        transactions: List<TransactionEntity>
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("ar"))
        val currencyFormat = NumberFormat.getNumberInstance(Locale("ar"))

        val sb = StringBuilder()
        sb.append("📋 *كشف حساب مالي - دفتر الديون*\n")
        sb.append("👤 *الزبون:* ${item.customer.name}\n")
        if (item.customer.phone.isNotBlank()) sb.append("📱 *الهاتف:* ${item.customer.phone}\n")
        sb.append("📅 *تاريخ الكشف:* ${dateFormat.format(Date())}\n")
        sb.append("─────────────────────\n")

        transactions.take(10).forEach { tx ->
            val typeStr = if (tx.type == TransactionType.DEBT) "❌ دين علي" else "✅ تسديد"
            val amtStr = currencyFormat.format(tx.amount)
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val desc = if (tx.description.isNotBlank()) " (${tx.description})" else ""
            sb.append("• $typeStr: $amtStr $desc - $dateStr\n")
        }

        sb.append("─────────────────────\n")
        val netAmt = currencyFormat.format(Math.abs(item.netBalance))
        if (item.netBalance > 0) {
            sb.append("⚠️ *المبلغ المستحق المتبقي عليك:* $netAmt\n")
        } else if (item.netBalance < 0) {
            sb.append("🟢 *رصيد فائض لك عندنا:* $netAmt\n")
        } else {
            sb.append("✅ *الحساب خالص ومتعادل بالكامل*\n")
        }
        sb.append("\nشكراً لتعاملكم معنا 🙏")
        return sb.toString()
    }
}
