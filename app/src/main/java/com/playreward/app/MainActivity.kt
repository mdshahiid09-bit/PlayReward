package com.playreward.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.playreward.app.ui.components.FloatingToast
import com.playreward.app.ui.components.IconBadge
import com.playreward.app.ui.screens.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.theme.PlayRewardTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()
            PlayRewardTheme(themeMode = themeMode) {
                PlayRewardRoot(vm = vm, onExitApp = { finish() })
            }
        }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun PlayRewardRoot(vm: MainViewModel, onExitApp: () -> Unit) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()

    val currentUid by vm.currentUid.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val appConfig by vm.appConfig.collectAsState()
    val referralConfig by vm.referralConfig.collectAsState()
    val onboardingDone by vm.onboardingDone.collectAsState()
    val onboardingPages by vm.onboardingPages.collectAsState()
    val startupPopup by vm.startupPopup.collectAsState()
    val popupShown by vm.popupShownThisSession.collectAsState()
    val availableUpdate by vm.availableUpdate.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val localBypassUid by vm.localAdminBypassUid.collectAsState()
    val toast by vm.toast.collectAsState()

    var splashDone by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isAdminBypassed = remember(appConfig, currentUid, localBypassUid) {
        val uid = currentUid.orEmpty()
        (uid.isNotBlank() && appConfig.adminBypassUids[uid] == true) ||
            (localBypassUid.isNotBlank() && appConfig.adminBypassUids[localBypassUid] == true)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        when {
            !splashDone -> {
                SplashScreen(onFinished = { splashDone = true })
            }
            availableUpdate != null && appConfig.forceUpdateEnabled -> {
                ForcedUpdateScreen(
                    release = availableUpdate!!,
                    downloadState = downloadState,
                    onStartDownload = { vm.startUpdateDownload(availableUpdate!!) }
                )
            }
            appConfig.maintenanceMode && !isAdminBypassed -> {
                MaintenanceScreen(
                    config = appConfig,
                    currentUid = currentUid,
                    onSaveBypassUid = { vm.saveAdminMaintenanceBypass(it) }
                )
            }
            !onboardingDone -> {
                OnboardingScreen(
                    adminPages = onboardingPages,
                    onComplete = { vm.completeOnboarding() }
                )
            }
            currentUid.isNullOrBlank() -> {
                AuthFlowScreen(
                    viewModel = vm,
                    onAuthSuccess = { vm.syncAuthUid() }
                )
            }
            else -> {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route ?: "home"

                // Exact 5-Tab Bottom Navigation Bar requested by user
                val bottomTabs = remember {
                    listOf(
                        BottomTab("tasks", "Tasks", Icons.Rounded.TaskAlt),
                        BottomTab("play", "Play & Earn", Icons.Rounded.SportsEsports),
                        BottomTab("home", "Home", Icons.Rounded.Home),
                        BottomTab("wallet", "Wallet", Icons.Rounded.AccountBalanceWallet),
                        BottomTab("profile", "Profile", Icons.Rounded.Person)
                    )
                }
                val topLevelRoutes = remember { bottomTabs.map { it.route }.toSet() }

                BackHandler(enabled = currentRoute == "home") {
                    showExitDialog = true
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(drawerContainerColor = colors.card) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(290.dp)
                                    .verticalScroll(rememberScrollState())
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(16.dp)
                            ) {
                                // Drawer Profile Header
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.heroGradient).padding(18.dp)
                                ) {
                                    Column {
                                        Text("PlayReward", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.height(4.dp))
                                        Text(profile?.fullName?.ifBlank { "Player" } ?: "Player", color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold)
                                        Text("${profile?.coins ?: 0} Coins • ₹${String.format("%.2f", profile?.inrBalance ?: 0.0)}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.5.sp)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))

                                fun goDrawer(route: String) {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route) { launchSingleTop = true }
                                }

                                DrawerNavItem(Icons.Rounded.Home, "Home") { goDrawer("home") }
                                DrawerNavItem(Icons.Rounded.TaskAlt, "Tasks") { goDrawer("tasks") }
                                DrawerNavItem(Icons.Rounded.History, "Task History") { goDrawer("task_history") }
                                DrawerNavItem(Icons.Rounded.Quiz, "Play & Earn") { goDrawer("play") }
                                DrawerNavItem(Icons.Rounded.LocalFireDepartment, "Mega Offer") { goDrawer("mega_offer") }
                                DrawerNavItem(Icons.Rounded.Grid3x3, "Super Offer (Tic Tac Toe)") { goDrawer("super_offer") }
                                DrawerNavItem(Icons.Rounded.CardGiftcard, "Refer & Earn") { goDrawer("refer") }
                                DrawerNavItem(Icons.Rounded.EmojiEvents, "Leaderboard") { goDrawer("leaderboard") }
                                DrawerNavItem(Icons.Rounded.AccountBalanceWallet, "Wallet") { goDrawer("wallet") }
                                DrawerNavItem(Icons.Rounded.Notifications, "Notifications") { goDrawer("notifications") }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.cardBorder)
                                DrawerNavItem(Icons.Rounded.Settings, "Settings") { goDrawer("settings") }
                                DrawerNavItem(Icons.Rounded.SystemUpdate, "Check for Updates") {
                                    scope.launch { drawerState.close() }
                                    vm.checkUpdateManually()
                                }
                                DrawerNavItem(Icons.Rounded.Logout, "Sign Out", tint = colors.roseAccent) {
                                    scope.launch { drawerState.close() }
                                    vm.logout()
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = colors.canvas,
                        bottomBar = {
                            if (currentRoute in topLevelRoutes) {
                                Surface(
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        bottomTabs.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            val isHome = tab.route == "home"
                                            Column(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        if (currentRoute != tab.route) {
                                                            navController.navigate(tab.route) {
                                                                popUpTo("home") { saveState = true }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (isHome) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(if (selected) colors.heroGradient else androidx.compose.ui.graphics.Brush.linearGradient(listOf(colors.lavenderSoft, colors.lavenderSoft))),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(tab.icon, tab.label, tint = if (selected) Color.White else colors.primaryIndigo, modifier = Modifier.size(22.dp))
                                                    }
                                                } else {
                                                    Icon(
                                                        tab.icon,
                                                        tab.label,
                                                        tint = if (selected) colors.primaryIndigo else colors.textSecondary,
                                                        modifier = Modifier.size(23.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = tab.label,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) colors.primaryIndigo else colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = vm,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                                    onOpenTask = { id -> navController.navigate("task_detail/$id") }
                                )
                            }
                            composable("tasks") {
                                TasksScreen(
                                    viewModel = vm,
                                    onOpenTask = { id -> navController.navigate("task_detail/$id") },
                                    onOpenHistory = { navController.navigate("task_history") }
                                )
                            }
                            composable("task_detail/{taskId}", arguments = listOf(navArgument("taskId") { type = NavType.StringType })) {
                                TaskDetailScreen(
                                    taskId = it.arguments?.getString("taskId").orEmpty(),
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("task_history") {
                                TaskHistoryScreen(
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() },
                                    onOpenTask = { id -> navController.navigate("task_detail/$id") }
                                )
                            }
                            composable("play") {
                                PlayAndEarnScreen(
                                    viewModel = vm,
                                    onOpenCategory = { id, name -> navController.navigate("quiz_play/$id/$name") }
                                )
                            }
                            composable(
                                "quiz_play/{catId}/{catName}",
                                arguments = listOf(
                                    navArgument("catId") { type = NavType.StringType },
                                    navArgument("catName") { type = NavType.StringType }
                                )
                            ) {
                                QuizPlayScreen(
                                    categoryId = it.arguments?.getString("catId").orEmpty(),
                                    categoryName = it.arguments?.getString("catName").orEmpty(),
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("mega_offer") {
                                MegaOfferScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("super_offer") {
                                SuperOfferTicTacToeScreen(
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() },
                                    onEnterMatch = { tierId -> navController.navigate("tictactoe_match/$tierId") }
                                )
                            }
                            composable(
                                "tictactoe_match/{tierId}",
                                arguments = listOf(navArgument("tierId") { type = NavType.StringType })
                            ) {
                                TicTacToeMatchScreen(
                                    tierId = it.arguments?.getString("tierId").orEmpty(),
                                    viewModel = vm,
                                    onExitToLobby = { navController.popBackStack() }
                                )
                            }
                            composable("refer") {
                                ReferAndEarnScreen(
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() },
                                    onOpenReferralList = { filter -> navController.navigate("referrals/$filter") }
                                )
                            }
                            composable("referrals/{filter}", arguments = listOf(navArgument("filter") { type = NavType.StringType })) {
                                ReferralListScreen(
                                    filter = it.arguments?.getString("filter") ?: "ALL",
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("leaderboard") {
                                LeaderboardScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("notifications") {
                                NotificationsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("wallet") {
                                WalletScreen(
                                    viewModel = vm,
                                    onOpenPayoutSettings = { navController.navigate("payout_settings") }
                                )
                            }
                            composable("payout_settings") {
                                PayoutSettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = vm,
                                    onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
                                )
                            }
                            composable("edit_profile") {
                                EditProfileScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("settings") {
                                SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                            }
                            composable("legal/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) {
                                LegalPageScreen(
                                    pageType = it.arguments?.getString("type") ?: "about",
                                    viewModel = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }

                // Post-Google-Auth Referral Code & Personal Details Popups
                profile?.let { user ->
                    GooglePostAuthSetupPopups(
                        profile = user,
                        referralConfig = referralConfig,
                        appConfig = appConfig,
                        viewModel = vm
                    )
                }

                // Single Full-Screen Startup Popup Image (3s close button)
                if (!popupShown && startupPopup.enabled && startupPopup.imageUrl.isNotBlank()) {
                    StartupImagePopupOverlay(
                        config = startupPopup,
                        onDismiss = { vm.markPopupShown() }
                    )
                }

                // Exit App Confirmation Dialog
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        containerColor = colors.card,
                        icon = { Icon(Icons.Rounded.ExitToApp, null, tint = colors.primaryIndigo) },
                        title = { Text("Exit PlayReward?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                        text = { Text("Are you sure you want to close the app?", color = colors.textSecondary) },
                        confirmButton = {
                            TextButton(onClick = { showExitDialog = false; onExitApp() }) {
                                Text("Exit", color = colors.roseAccent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) { Text("Stay") }
                        }
                    )
                }
            }
        }

        // Global Floating Toast (Reference Video Style)
        FloatingToast(
            message = toast?.message,
            type = toast?.type ?: com.playreward.app.ui.components.ToastType.INFO,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 76.dp)
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = PRTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint ?: colors.primaryIndigo, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = tint ?: colors.textPrimary)
    }
}
