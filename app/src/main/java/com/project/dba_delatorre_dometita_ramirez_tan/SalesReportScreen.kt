package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    navController: NavController,
    viewModel: SalesReportViewModel
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
        viewModel.syncAndLoadSales()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController, drawerState) },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Sales Report", color = Color.White, fontWeight = FontWeight.Bold) },
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
                                    FilterButton(
                                        label = period,
                                        isSelected = selectedPeriod == period,
                                        onClick = {
                                            selectedPeriod = period
                                            if (period != "All") {
                                                viewModel.filterByPeriod(period)
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
                                            imageVector = androidx.compose.material.icons.filled.DateRange,
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
                                            imageVector = androidx.compose.material.icons.filled.DateRange,
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
                                            viewModel.filterSalesByRange(startStr, endStr)
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
                    if (viewModel.isLoading) {
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
                                // Total Revenue Card
                                SummarySalesCard(
                                    title = "Total Revenue",
                                    value = "₱${String.format("%.2f", viewModel.totalRevenue)}",
                                    subtitle = "$selectedPeriod Sales",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = Cappuccino
                                )

                                // Total Sold Card
                                SummarySalesCard(
                                    title = "Units Sold",
                                    value = "${viewModel.totalSold}",
                                    subtitle = "$selectedPeriod Sales",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = LatteCream
                                )
                            }
                        }

                        // ============= TOP PRODUCTS SECTION =============
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
                                    text = "Top Selling Products",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoDark
                                )

                                if (viewModel.topSales.value.isEmpty()) {
                                    Text(
                                        text = "No sales data for selected period",
                                        fontSize = 14.sp,
                                        color = Mocha,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    viewModel.topSales.value.take(5).forEachIndexed { index, item ->
                                        TopProductItem(
                                            rank = index + 1,
                                            productName = item.productName,
                                            unitsSold = item.totalSold,
                                            revenue = item.totalRevenue
                                        )
                                    }
                                }
                            }
                        }

                        // ============= SALES DETAILS LIST =============
                        item {
                            Text(
                                text = "Sales Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoDark
                            )
                        }

                        if (viewModel.salesList.isEmpty()) {
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
                                        text = "No sales found for this period",
                                        color = EspressoDark
                                    )
                                }
                            }
                        } else {
                            items(viewModel.salesList) { sale ->
                                SalesDetailCard(sale)
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
// Filter Button Component
// ============================================================================
@Composable
fun FilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CoffeeBrown else LightCoffee,
            contentColor = if (isSelected) Color.White else EspressoDark
        )
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================================
// Summary Card Component
// ============================================================================
@Composable
fun SummarySalesCard(
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
// Top Product Item Component
// ============================================================================
@Composable
fun TopProductItem(
    rank: Int,
    productName: String,
    unitsSold: Int,
    revenue: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank badge
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50)),
            color = CoffeeBrown
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Product details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EspressoDark
            )

            Text(
                text = "$unitsSold units sold",
                fontSize = 12.sp,
                color = Mocha
            )
        }

        // Revenue
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "₱${String.format("%.2f", revenue)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CoffeeBrown
            )
        }
    }
}

// ============================================================================
// Sales Detail Card Component
// ============================================================================
@Composable
fun SalesDetailCard(sale: Entity_SalesReport) {
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
                    text = sale.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoDark
                )

                Text(
                    text = sale.category,
                    fontSize = 11.sp,
                    color = Mocha
                )
            }

            Text(
                text = "₱${String.format("%.2f", sale.price * sale.quantity)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CoffeeBrown
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Qty: ${sale.quantity}",
                fontSize = 12.sp,
                color = Mocha
            )

            Text(
                text = sale.paymentMode,
                fontSize = 12.sp,
                color = Mocha,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(LatteCream)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Text(
                text = sale.orderDate.take(10),
                fontSize = 12.sp,
                color = Mocha
            )
        }
    }
}

// ============================================================================
// Simple Date Picker Dialog Component
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    title: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay().toInstant(
            org.threeten.bp.ZoneOffset.UTC
        ).toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val threeTenDate = org.threeten.bp.LocalDate.of(
                            selectedDate.year,
                            selectedDate.monthValue,
                            selectedDate.dayOfMonth
                        )
                        onDateSelected(threeTenDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)
            ) {
                Text("OK", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
