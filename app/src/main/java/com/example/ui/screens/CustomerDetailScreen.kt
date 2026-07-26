package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerWithBalance
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.TransactionItem
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomerDetailScreen(
    customerWithBalance: CustomerWithBalance?,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onOpenVoiceDialog: () -> Unit,
    onAddManualTransaction: (customerId: Long, type: TransactionType, amount: Double, desc: String) -> Unit,
    onDeleteCustomer: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onGenerateStatementText: (CustomerWithBalance, List<TransactionEntity>) -> String
) {
    if (customerWithBalance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على بيانات الزبون")
        }
        return
    }

    val context = LocalContext.current
    val customer = customerWithBalance.customer
    val balance = customerWithBalance.netBalance
    val isOwed = balance > 0
    val isSettled = balance == 0.0

    val currencyFormatter = NumberFormat.getNumberInstance(Locale("ar"))

    var isManualAddOpen by remember { mutableStateOf(false) }
    var isDeleteConfirmOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Back Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                }

                Text(
                    text = "كشف حساب زبون 👤",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { isDeleteConfirmOpen = true },
                    modifier = Modifier.testTag("delete_customer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الزبون",
                        tint = DebtRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Summary Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwed) DebtRedContainer else if (isSettled) MaterialTheme.colorScheme.surface else PaymentGreenContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = customer.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (customer.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (customer.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ملاحظات: ${customer.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Net Balance Metric
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isOwed) "إجمالي المتبقي عليه (دين)"
                                else if (isSettled) "الحساب خالص ومتعادل"
                                else "رصيد دائن للزبون (له عندنا)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOwed) DebtRed else if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else PaymentGreen
                            )
                            Text(
                                text = "${currencyFormatter.format(Math.abs(balance))} ر.س",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwed) DebtRed else if (isSettled) MaterialTheme.colorScheme.onSurface else PaymentGreen
                            )
                        }

                        // Share WhatsApp Statement Button
                        Button(
                            onClick = {
                                val statementText = onGenerateStatementText(customerWithBalance, transactions)
                                shareStatement(context, statementText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("كشف حساب", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Voice Add & Manual Add)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenVoiceDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدخال صوتي سريع", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { isManualAddOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل يدوي", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "سجل حركة الحساب (${transactions.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد حركات مسجلة لهذا الزبون حتى الآن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions, key = { "cust_tx_${it.id}" }) { tx ->
                TransactionItem(
                    transaction = tx,
                    onDelete = { onDeleteTransaction(tx) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    // Manual Add Dialog
    if (isManualAddOpen) {
        var txType by remember { mutableStateOf(TransactionType.DEBT) }
        var amountText by remember { mutableStateOf("") }
        var descText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isManualAddOpen = false },
            title = { Text("تسجيل حركة ماليّة لـ ${customer.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = txType == TransactionType.DEBT,
                            onClick = { txType = TransactionType.DEBT },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("❌ دين علي")
                        }
                        SegmentedButton(
                            selected = txType == TransactionType.PAYMENT,
                            onClick = { txType = TransactionType.PAYMENT },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("✅ تسديد واصل")
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("تفاصيل / ملاحظات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddManualTransaction(customer.id, txType, amt, descText)
                            isManualAddOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ الحركة")
                }
            },
            dismissButton = {
                TextButton(onClick = { isManualAddOpen = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (isDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmOpen = false },
            title = { Text("تأكيد حذف الزبون") },
            text = { Text("هل أنت تأكد من حذف الزبون (${customer.name}) وكافة حركات حسابه المسجلة؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleteConfirmOpen = false
                        onDeleteCustomer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) {
                    Text("حذف الزبون بالحركات")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmOpen = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private fun shareStatement(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة كشف الحساب")
    context.startActivity(shareIntent)
}
