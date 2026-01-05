


package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dashboard(navController: NavController, viewModel: SalesReportViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf("Week") }
    val topSales = viewModel.topSales.value
    val LatteCream = Color(0xFFF3E5AB)
    val LightCoffee = Color(0xFFFAF1E6)
    val Mocha = Color(0xFF837060)
    val Cappuccino = Color(0xFFDDBEA9)
    val CoffeeBrown = Color(0xFF6F4E37)
    val EspressoDark = Color(0xFF4B3621)
    val BackgroundCoffee = Color(0xFFFFF8F0)
    val Latte = Color(0xFFF5E6DA)

    // ✅ FIX: Refresh sales data when dashboard is opened
    LaunchedEffect(Unit) {
        android.util.Log.d("Dashboard", "🔄 Refreshing sales data...")
        viewModel.syncAndLoadSales()
        viewModel.filterByPeriod(selectedFilter)
        android.util.Log.d("Dashboard", "✅ Sales data refreshed")
        android.util.Log.d("Dashboard", "   Total Sales: ${viewModel.totalRevenue}")
        android.util.Log.d("Dashboard", "   Total Sold: ${viewModel.totalSold}")
        android.util.Log.d("Dashboard", "   Top Sales Count: ${viewModel.topSales.value.size}")
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightCoffee)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SidebarDrawer(navController, drawerState)
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        shadowElevation = 8.dp,
                    ) {
                        TopAppBar(
                            title = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text("Overview", color = Color.White)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            },

                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = CoffeeBrown,
                                titleContentColor = Color.White
                            )

                        )

                    }
                },
                content = { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .background(LightCoffee)
                            .verticalScroll(scrollState)
                    ) {
                        // Filter Toggle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3D3BD))
                                    .padding(4.dp)
                            ) {
                                listOf("Today", "Week", "Month").forEach { filter ->
                                    val isSelected = filter == selectedFilter
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF6F4E37) else Color.Transparent)
                                            .clickable {
                                                selectedFilter = filter
                                                viewModel.filterByPeriod(filter)
                                            }
                                            .padding(horizontal = 20.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            color = if (isSelected) Color.White else Color.Black,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))




                        // Two top summary boxes
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .padding(4.dp)
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Cappuccino, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text("Sold Products", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoffeeBrown)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${viewModel.totalSold}", fontSize = 15.sp, color = CoffeeBrown)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .padding(4.dp)
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp)) // <- Add this
                                    .background(Cappuccino),

                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text("Total Sales", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoffeeBrown)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₱${"%.2f".format(viewModel.totalRevenue)}", fontSize = 15.sp, color = CoffeeBrown)

                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Top Sales donut chart
                        Text(
                            "Top Sales",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(15.dp))
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = Stroke(width = 24f, cap = StrokeCap.Round)
                                val totalRevenue = topSales.sumOf { it.totalRevenue }
                                var startAngle = -90f

                                val chartColors = listOf(
                                    Color(0xFF6F4E37), // Rich Coffee Brown
                                    Color(0xFFD7A86E), // Soft Caramel
                                    Color(0xFFB08968), // Milk Tea Beige
                                    Color(0xFFA1887F), // Taupe
                                    Color(0xFF8D6E63)  // Mocha
                                )

                                topSales.take(5).forEachIndexed { index, item ->
                                    val sweepAngle = if (totalRevenue > 0)
                                        (item.totalRevenue / totalRevenue * 360f).toFloat()
                                    else 0f

                                    drawArc(
                                        color = chartColors.getOrElse(index) { Color.Gray },
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = stroke
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                        }

                        // Top Sales breakdown
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {

                            if (topSales.isEmpty()) {
                                Text("No sales data available.")
                            } else {
                                // Dynamically generate SalesItem components for top 5 sales
                                topSales.take(5).forEachIndexed { index, item ->
                                    val colors = listOf(
                                        Color(0xFF6F4E37), // Rich Coffee Brown
                                        Color(0xFFD7A86E), // Soft Caramel
                                        Color(0xFFB08968), // Milk Tea Beige
                                        Color(0xFFA1887F), // Taupe
                                        Color(0xFF8D6E63)  // Mocha
                                    )

                                    SalesItem(
                                        name = "${index + 1}. ${item.productName}",
                                        price =  "₱%.2f".format(item.totalRevenue),
                                        color = colors.getOrElse(index) { Color.Gray }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .padding(4.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .background(Cappuccino, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        )
                        {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Best Seller:",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoffeeBrown, modifier = Modifier.padding(start = 16.dp)
                                )
                                Text(
                                    viewModel.topSales.value.firstOrNull()?.productName ?: "N/A",
                                    fontSize = 15.sp,
                                    color = EspressoDark,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun SalesItem(name: String, price: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, fontSize = 16.sp)
        }
        Text(price, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SidebarDrawer(navController: NavController, drawerState: DrawerState) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var expandedReports by remember { mutableStateOf(false) }

    // ✅ Get menu items based on user role
    val menuItems = RoleManager.getMenuItemsForRole()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Brush.verticalGradient(
                colors = listOf(Color(0xFFB89E8C), Color(0xFF7B5E57))
            ))
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(3.dp, Color(0xFF4B3832), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Display user info and role
        Text(
            text = UserSession.getUserFullName(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = RoleManager.getCurrentUserRole(),
            fontSize = 14.sp,
            color = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ Display only authorized menu items
        menuItems.forEach { (title, icon) ->
            DrawerMenuItem(title, icon, navController, drawerState) {
                if (title == "Log Out") {
                    showLogoutDialog = true
                }
            }
        }

        // ✅ Reports dropdown menu (Manager only)
        if (RoleManager.getCurrentUserRole().equals("Manager", ignoreCase = true)) {
            ExpandableDrawerMenuItem(
                title = "Reports",
                icon = Icons.Default.BarChart,
                isExpanded = expandedReports,
                onExpandClick = { expandedReports = !expandedReports },
                subItems = listOf(
                    "Sales Report" to { navController.navigate(Routes.R_SalesReport.routes) },
                    "Waste Report" to { navController.navigate(Routes.R_WasteReport.routes) }
                ),
                navController = navController,
                drawerState = drawerState
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Confirm Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF4B3832)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to log out?",
                        fontSize = 16.sp,
                        color = Color(0xFF4B3832)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val username = UserSession.currentUser?.Entity_username ?: "Unknown"
                            val fullName = UserSession.getUserFullName()

                            AuditHelper.logLogout(username, fullName)
                            UserSession.logout()

                            showLogoutDialog = false
                            navController.navigate(Routes.R_Login.routes) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3832)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Yes", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        border = BorderStroke(1.dp, Color(0xFF4B3832)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("No", color = Color(0xFF4B3832))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}


@Composable
fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    navController: NavController,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    onLogoutClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selected = currentRoute?.contains(title.replace(" ", ""), ignoreCase = true) == true

    val backgroundColor = if (selected) Color(0xFFD7CCC8) else Color.Transparent
    val textColor = if (selected) Color(0xFF4B3832) else Color(0xFF4B3832)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                when (title) {
                    "Log Out" -> onLogoutClick()
                    "Overview" -> {
                        navController.navigate(Routes.R_DashboardScreen.routes)
                        scope.launch { drawerState.close() }
                    }
                    "Order Process" -> {
                        navController.navigate(Routes.OrderProcess.routes)
                        scope.launch { drawerState.close() }
                    }
                    "Inventory List" -> {
                        navController.navigate(Routes.R_InventoryList.routes)
                        scope.launch { drawerState.close() }
                    }
                }
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = textColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================================
// Expandable Drawer Menu Item Component (for Reports with sub-items)
// ============================================================================
@Composable
fun ExpandableDrawerMenuItem(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    subItems: List<Pair<String, () -> Unit>>,
    navController: NavController,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Parent menu item
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onExpandClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF4B3832),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    color = Color(0xFF4B3832),
                    fontWeight = FontWeight.Normal
                )
            }

            // Expand/collapse arrow
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF4B3832),
                modifier = Modifier.size(20.dp)
            )
        }

        // Sub-items (shown when expanded)
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            ) {
                subItems.forEach { (subTitle, onSubItemClick) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSubItemClick()
                                scope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        // Dot indicator
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4B3832), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = subTitle,
                            fontSize = 13.sp,
                            color = Color(0xFF4B3832)
                        )
                    }
                }
            }
        }
    }
}