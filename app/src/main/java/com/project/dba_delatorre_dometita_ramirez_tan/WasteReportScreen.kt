package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

// ============================================================================
// Colors - Dashboard Theme
// ============================================================================
private val LatteCream = Color(0xFFF3E5AB)
private val LightCoffee = Color(0xFFFAF1E6)
private val Mocha = Color(0xFF837060)
private val Cappuccino = Color(0xFFDDBEA9)
private val CoffeeBrown = Color(0xFF6F4E37)
private val EspressoDark = Color(0xFF4B3621)
private val BackgroundCoffee = Color(0xFFFFF8F0)
private val Latte = Color(0xFFF5E6DA)
private val WasteRed = Color(0xFFD32F2F)
private val WarningOrange = Color(0xFFF57C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteReportScreen(
    navController: NavController,
    wasteLogViewModel: WasteLogViewModel
) {
    // UI State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf("Today") }

    // Date range state for "All" filter
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusMonths(1)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Load data on screen entry
    LaunchedEffect(Unit) {
        wasteLogViewModel.loadAllWasteLogs()
        wasteLogViewModel.filterByPeriod("Today")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController, drawerState) },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Waste Report", color = Color.White, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CoffeeBrown
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundCoffee)
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ============= PERIOD FILTER BUTTONS =============
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // First row: Today, Week, Month, All
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LightCoffee),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val periods = listOf("Today", "Week", "Month", "All")
                                periods.forEach { period ->
                                    ReportFilterButton(
                                        label = period,
                                        isSelected = selectedPeriod == period,
                                        onClick = {
                                            selectedPeriod = period
                                            if (period != "All") {
                                                wasteLogViewModel.filterByPeriod(period)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Date range pickers - shown when "All" is selected
                            if (selectedPeriod == "All") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Cappuccino)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Select Date Range",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EspressoDark
                                    )

                                    // Start Date Picker
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .clickable { showStartDatePicker = true }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "From: ${customStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                            fontSize = 12.sp,
                                            color = EspressoDark
                                        )
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Pick start date",
                                            tint = CoffeeBrown,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // End Date Picker
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .clickable { showEndDatePicker = true }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "To: ${customEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                            fontSize = 12.sp,
                                            color = EspressoDark
                                        )
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Pick end date",
                                            tint = CoffeeBrown,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Apply button
                                    Button(
                                        onClick = {
                                            val startStr = customStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                            val endStr = customEndDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                            wasteLogViewModel.filterByDateRange(startStr, endStr)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Apply Filter",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ============= LOADING STATE =============
                    if (wasteLogViewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CoffeeBrown)
                            }
                        }
                    } else {
                        // ============= SUMMARY CARDS =============
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Total Waste Quantity Card
                                SummaryWasteCard(
                                    title = "Units Wasted",
                                    value = "${wasteLogViewModel.totalWasteQuantity}",
                                    subtitle = "$selectedPeriod Waste",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = WarningOrange.copy(alpha = 0.3f)
                                )

                                // Total Waste Records Card
                                SummaryWasteCard(
                                    title = "Waste Records",
                                    value = "${wasteLogViewModel.wasteLogs.size}",
                                    subtitle = "$selectedPeriod Count",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = WasteRed.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // ============= WASTE REASONS BREAKDOWN =============
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Latte)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Waste by Reason",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoDark
                                )

                                val wasteByReason = wasteLogViewModel.wasteLogs
                                    .groupingBy { it.reason }
                                    .eachCount()
                                    .toList()
                                    .sortedByDescending { it.second }

                                if (wasteByReason.isEmpty()) {
                                    Text(
                                        text = "No waste records for selected period",
                                        fontSize = 14.sp,
                                        color = Mocha,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    wasteByReason.forEach { (reason, count) ->
                                        WasteReasonItem(
                                            reason = reason,
                                            count = count,
                                            total = wasteLogViewModel.wasteLogs.size
                                        )
                                    }
                                }
                            }
                        }

                        // ============= WASTE DETAILS LIST =============
                        item {
                            Text(
                                text = "Waste Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoDark
                            )
                        }

                        if (wasteLogViewModel.wasteLogs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Cappuccino),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No waste records for this period",
                                        color = EspressoDark
                                    )
                                }
                            }
                        } else {
                            items(wasteLogViewModel.wasteLogs) { wasteLog ->
                                WasteDetailCard(wasteLog)
                            }
                        }

                        // Bottom padding
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    )

    // ============================================================================
    // Date Picker Dialogs
    // ============================================================================
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            title = "Select Start Date",
            onDateSelected = { selectedDate ->
                customStartDate = selectedDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            initialDate = customStartDate
        )
    }

    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            title = "Select End Date",
            onDateSelected = { selectedDate ->
                customEndDate = selectedDate
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            initialDate = customEndDate
        )
    }
}

// ============================================================================
// Summary Card Component
// ============================================================================
@Composable
fun SummaryWasteCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = EspressoDark,
            fontWeight = FontWeight.Normal
        )

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EspressoDark
        )

        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Mocha
        )
    }
}

// ============================================================================
// Waste Reason Item Component
// ============================================================================
@Composable
fun WasteReasonItem(
    reason: String,
    count: Int,
    total: Int
) {
    val percentage = if (total > 0) (count * 100) / total else 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reason,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EspressoDark
            )

            Text(
                text = "$count records ($percentage%)",
                fontSize = 12.sp,
                color = Mocha
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = percentage / 100f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WasteRed)
            )
        }
    }
}

// ============================================================================
// Waste Detail Card Component
// ============================================================================
@Composable
fun WasteDetailCard(wasteLog: Entity_WasteLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wasteLog.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoDark
                )

                Text(
                    text = wasteLog.category,
                    fontSize = 11.sp,
                    color = Mocha
                )
            }

            Text(
                text = "${wasteLog.quantity} units",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WasteRed
            )
        }

        // Reason badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(WarningOrange.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = wasteLog.reason,
                fontSize = 11.sp,
                color = EspressoDark
            )
        }

        // Date and recorded by
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = wasteLog.wasteDate.take(10),
                fontSize = 12.sp,
                color = Mocha
            )

            Text(
                text = "By: ${wasteLog.recordedBy}",
                fontSize = 12.sp,
                color = Mocha
            )
        }
    }
}


