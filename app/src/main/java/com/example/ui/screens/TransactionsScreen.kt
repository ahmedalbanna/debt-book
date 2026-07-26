package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CustomerWithBalance
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldPrimary

enum class TransactionFilter {
    ALL,
    DEBTS_ONLY,
    PAYMENTS_ONLY
}

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    customers: List<CustomerWithBalance>,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    var filter by remember { mutableStateOf(TransactionFilter.ALL) }

    val filteredTransactions = transactions.filter { tx ->
        when (filter) {
            TransactionFilter.ALL -> true
            TransactionFilter.DEBTS_ONLY -> tx.type == TransactionType.DEBT
            TransactionFilter.PAYMENTS_ONLY -> tx.type == TransactionType.PAYMENT
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "سجل كافة الحركات المالية 📑",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == TransactionFilter.ALL,
                onClick = { filter = TransactionFilter.ALL },
                label = { Text("الكل (${transactions.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = filter == TransactionFilter.DEBTS_ONLY,
                onClick = { filter = TransactionFilter.DEBTS_ONLY },
                label = { Text("الديون فقط (${transactions.count { it.type == TransactionType.DEBT }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = filter == TransactionFilter.PAYMENTS_ONLY,
                onClick = { filter = TransactionFilter.PAYMENTS_ONLY },
                label = { Text("التسديدات فقط (${transactions.count { it.type == TransactionType.PAYMENT }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTransactions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد حركات مسجلة تطابق التصفية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTransactions, key = { "global_tx_${it.id}" }) { tx ->
                    val custName = customers.find { it.customer.id == tx.customerId }?.customer?.name
                    TransactionItem(
                        transaction = tx,
                        customerName = custName,
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}
