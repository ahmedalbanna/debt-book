package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBT,    // دين / سجل على الزبون
    PAYMENT  // تسديد / واصل من الزبون
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val type: TransactionType,
    val amount: Double,
    val description: String = "",
    val rawVoiceText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalDebt: Double,
    val totalPayment: Double,
    val netBalance: Double, // totalDebt - totalPayment (Positive = Customer Owes Us, Negative = Customer Credit)
    val transactionCount: Int,
    val lastTransactionDate: Long?
)

data class ParsedVoiceInput(
    val rawText: String,
    val detectedCustomerName: String,
    val matchedCustomerId: Long? = null,
    val amount: Double,
    val transactionType: TransactionType,
    val notes: String = "",
    val confidenceScore: Float = 0.9f
)
