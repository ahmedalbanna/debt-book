package com.example.data

import com.example.parser.VoiceCommandParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class LedgerRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    private val voiceCommandParser = VoiceCommandParser()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    val totalDebts: Flow<Double> = transactionDao.getTotalDebtsFlow()
    val totalPayments: Flow<Double> = transactionDao.getTotalPaymentsFlow()

    val customersWithBalance: Flow<List<CustomerWithBalance>> = combine(
        allCustomers,
        allTransactions
    ) { customers, transactions ->
        customers.map { customer ->
            val customerTransactions = transactions.filter { it.customerId == customer.id }
            val debt = customerTransactions.filter { it.type == TransactionType.DEBT }.sumOf { it.amount }
            val payment = customerTransactions.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
            val lastTxDate = customerTransactions.maxOfOrNull { it.timestamp }

            CustomerWithBalance(
                customer = customer,
                totalDebt = debt,
                totalPayment = payment,
                netBalance = debt - payment,
                transactionCount = customerTransactions.size,
                lastTransactionDate = lastTxDate
            )
        }.sortedByDescending { it.netBalance }
    }

    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForCustomer(customerId)
    }

    suspend fun parseVoiceCommand(rawVoiceText: String): ParsedVoiceInput {
        val customers = allCustomers.first()
        return voiceCommandParser.parseArabicCommand(rawVoiceText, customers)
    }

    suspend fun addCustomer(name: String, phone: String = "", notes: String = ""): Long {
        val entity = CustomerEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim())
        return customerDao.insertCustomer(entity)
    }

    suspend fun getOrCreateCustomerByName(name: String): CustomerEntity {
        val existing = allCustomers.first().find {
            it.name.equals(name.trim(), ignoreCase = true)
        }
        if (existing != null) return existing

        val id = addCustomer(name.trim())
        return CustomerEntity(id = id, name = name.trim())
    }

    suspend fun addTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String = "",
        rawVoiceText: String? = null
    ): Long {
        val tx = TransactionEntity(
            customerId = customerId,
            type = type,
            amount = amount,
            description = description.trim(),
            rawVoiceText = rawVoiceText
        )
        return transactionDao.insertTransaction(tx)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        customerDao.deleteTransactionsForCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun seedSampleDataIfEmpty() {
        val current = allCustomers.first()
        if (current.isEmpty()) {
            val c1Id = customerDao.insertCustomer(CustomerEntity(name = "محمد عبده", phone = "0501234567", notes = "زبون دائم - حارة السوق"))
            val c2Id = customerDao.insertCustomer(CustomerEntity(name = "عبدالكريم العلي", phone = "0559876543", notes = "مطعم الأمانة"))
            val c3Id = customerDao.insertCustomer(CustomerEntity(name = "أحمد المقطري", phone = "0561122334", notes = "محل قطع الغيار"))
            val c4Id = customerDao.insertCustomer(CustomerEntity(name = "فاطمة الزهراء", phone = "0543322110", notes = "منزل مجاور"))

            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = c1Id,
                    type = TransactionType.DEBT,
                    amount = 25000.0,
                    description = "كرتون زيت وعلبة حليب",
                    rawVoiceText = "سجل على محمد عبده دين خمسة وعشرين ألف",
                    timestamp = now - 2 * dayMs
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = c1Id,
                    type = TransactionType.PAYMENT,
                    amount = 5000.0,
                    description = "دفعة نقداً",
                    rawVoiceText = "واصل من محمد عبده 5000",
                    timestamp = now - dayMs
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = c2Id,
                    type = TransactionType.DEBT,
                    amount = 15000.0,
                    description = "طلب بضاعة أسبوعية",
                    rawVoiceText = "على عبدالكريم 15000",
                    timestamp = now - 3 * dayMs
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = c2Id,
                    type = TransactionType.PAYMENT,
                    amount = 15000.0,
                    description = "تسديد بالكامل",
                    rawVoiceText = "سدد عبدالكريم 15000 تسديد بالكامل",
                    timestamp = now - 4 * 3600 * 1000L
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = c3Id,
                    type = TransactionType.DEBT,
                    amount = 8500.0,
                    description = "باقي حساب المشتريات",
                    rawVoiceText = "تسجيل دين على أحمد 8500",
                    timestamp = now - 12 * 3600 * 1000L
                )
            )
        }
    }
}
