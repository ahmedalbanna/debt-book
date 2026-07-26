package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speech.SpeechState
import com.example.ui.components.VoiceInputDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.VoiceGuideScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LedgerAppTheme
import com.example.viewmodel.LedgerViewModel

enum class NavTab {
    DASHBOARD,
    CUSTOMERS,
    CUSTOMER_DETAIL,
    TRANSACTIONS,
    VOICE_GUIDE
}

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerAppTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    // Collect States
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val totalDebts by viewModel.totalDebts.collectAsStateWithLifecycle()
    val totalPayments by viewModel.totalPayments.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val selectedCustomerWithBalance by viewModel.selectedCustomerWithBalance.collectAsStateWithLifecycle()
    val selectedCustomerTransactions by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()

    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val speechState by viewModel.speechManager.speechState.collectAsStateWithLifecycle()
    val volumeLevel by viewModel.speechManager.volumeLevel.collectAsStateWithLifecycle()

    // Permission launcher for record audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        }
    }

    fun checkAndStartListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.DASHBOARD,
                    onClick = { currentTab = NavTab.DASHBOARD },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                    label = { Text("الرئيسية", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.CUSTOMERS || currentTab == NavTab.CUSTOMER_DETAIL,
                    onClick = { currentTab = NavTab.CUSTOMERS },
                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = null) },
                    label = { Text("الزبائن", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("tab_customers")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.TRANSACTIONS,
                    onClick = { currentTab = NavTab.TRANSACTIONS },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("الحركات", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("tab_transactions")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.VOICE_GUIDE,
                    onClick = { currentTab = NavTab.VOICE_GUIDE },
                    icon = { Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null) },
                    label = { Text("الدليل الصوتي", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("tab_voice_guide")
                )
            }
        },
        floatingActionButton = {
            if (!voiceState.isOpen) {
                FloatingActionButton(
                    onClick = { viewModel.openVoiceDialog() },
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("global_mic_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "إدخال صوتي سريع"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> {
                    DashboardScreen(
                        totalDebts = totalDebts,
                        totalPayments = totalPayments,
                        customersWithBalance = customersWithBalance,
                        recentTransactions = allTransactions,
                        onOpenVoiceDialog = { viewModel.openVoiceDialog() },
                        onSelectCustomer = { customerId ->
                            viewModel.selectCustomer(customerId)
                            currentTab = NavTab.CUSTOMER_DETAIL
                        },
                        onNavigateToCustomers = { currentTab = NavTab.CUSTOMERS },
                        onNavigateToTransactions = { currentTab = NavTab.TRANSACTIONS },
                        onNavigateToVoiceGuide = { currentTab = NavTab.VOICE_GUIDE },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                    )
                }

                NavTab.CUSTOMERS -> {
                    CustomersScreen(
                        customers = filteredCustomers,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSelectCustomer = { customerId ->
                            viewModel.selectCustomer(customerId)
                            currentTab = NavTab.CUSTOMER_DETAIL
                        },
                        onCreateCustomer = { name, phone, notes ->
                            viewModel.createCustomer(name, phone, notes)
                        }
                    )
                }

                NavTab.CUSTOMER_DETAIL -> {
                    CustomerDetailScreen(
                        customerWithBalance = selectedCustomerWithBalance,
                        transactions = selectedCustomerTransactions,
                        onBack = { currentTab = NavTab.CUSTOMERS },
                        onOpenVoiceDialog = { viewModel.openVoiceDialog() },
                        onAddManualTransaction = { id, type, amount, desc ->
                            viewModel.addManualTransaction(id, type, amount, desc)
                        },
                        onDeleteCustomer = {
                            selectedCustomerWithBalance?.customer?.let { cust ->
                                viewModel.deleteCustomer(cust)
                                currentTab = NavTab.CUSTOMERS
                            }
                        },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onGenerateStatementText = { item, txs ->
                            viewModel.generateWhatsAppStatementText(item, txs)
                        }
                    )
                }

                NavTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = allTransactions,
                        customers = customersWithBalance,
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                    )
                }

                NavTab.VOICE_GUIDE -> {
                    VoiceGuideScreen(
                        onBack = { currentTab = NavTab.DASHBOARD },
                        onOpenVoiceDialog = { viewModel.openVoiceDialog() }
                    )
                }
            }
        }
    }

    // Fast Voice Input Modal Sheet
    VoiceInputDialog(
        state = voiceState,
        speechState = speechState,
        volumeLevel = volumeLevel,
        onStartListening = { checkAndStartListening() },
        onStopListening = { viewModel.stopVoiceListening() },
        onSimulatePreset = { phrase -> viewModel.simulateVoicePreset(phrase) },
        onFieldChange = { name, amount, type, notes ->
            viewModel.updateVoiceFields(name, amount, type, notes)
        },
        onConfirmSave = { viewModel.confirmAndSaveVoiceTransaction() },
        onDismiss = { viewModel.closeVoiceDialog() }
    )
}
