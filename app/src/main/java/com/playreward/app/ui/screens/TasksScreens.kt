package com.playreward.app.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playreward.app.R
import com.playreward.app.data.model.AppTask
import com.playreward.app.data.model.UserTaskRecord
import com.playreward.app.notifications.LocalNotificationHelper
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    onOpenTask: (String) -> Unit,
    onOpenHistory: () -> Unit
) {
    val colors = PRTheme.colors
    val appConfig by viewModel.appConfig.collectAsState()
    val categories by viewModel.taskCategories.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val userTasks by viewModel.userTasks.collectAsState()

    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var visibleCount by remember { mutableIntStateOf(15) }

    val activeIds = remember(userTasks) { userTasks.map { it.taskId }.toSet() }
    val availableTasks = remember(allTasks, activeIds, selectedCategoryId) {
        allTasks.filter { t ->
            t.id !in activeIds && (selectedCategoryId == "ALL" || t.categoryId == selectedCategoryId)
        }
    }
    val pagedTasks = remember(availableTasks, visibleCount) { availableTasks.take(visibleCount) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Header Row + History Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AssignmentTurnedIn, colors.amberSoft, colors.amberAccent, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Tasks", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text("${availableTasks.size} available", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                Surface(
                    modifier = Modifier.bounceClick(onClick = onOpenHistory),
                    shape = CircleShape,
                    color = colors.lavenderSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.History, null, tint = colors.primaryIndigo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("History", color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Admin-Managed Tasks Banner Card
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.heroGradient).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EARN REAL REWARDS", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(appConfig.tasksHeaderText, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Rounded.Stars, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(36.dp))
                }
            }
        }

        // Category Filter Chips
        if (categories.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == "ALL",
                            onClick = { selectedCategoryId = "ALL" },
                            label = { Text("All Tasks") }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        if (availableTasks.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.TaskAlt,
                    title = "No Available Tasks Right Now",
                    subtitle = "New tasks published from the admin panel appear here in real time. Check your Task History for pending items.",
                    actionLabel = "View Task History",
                    onAction = onOpenHistory
                )
            }
        } else {
            items(pagedTasks, key = { it.id }) { task ->
                AvailableTaskCard(task = task, onClick = { onOpenTask(task.id) })
            }
            if (visibleCount < availableTasks.size) {
                item {
                    LaunchedEffect(visibleCount) { visibleCount += 15 }
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = colors.primaryIndigo)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableTaskCard(task: AppTask, onClick: () -> Unit) {
    val colors = PRTheme.colors
    PRCard(onClick = onClick, cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (task.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = task.iconUrl,
                        contentDescription = task.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    IconBadge(Icons.Rounded.Apps, colors.lavenderSoft, colors.primaryIndigo, size = 50.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title.ifBlank { task.appName }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(task.appName, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = colors.amberSoft) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (task.currency != "INR") {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        val prize = if (task.currency == "INR") "₹${task.prizeAmount.toInt()}" else "${task.prizeAmount.toInt()}"
                        Text(prize, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
            if (task.badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    task.badges.take(3).forEach { badge ->
                        Surface(shape = CircleShape, color = colors.lavenderSoft) {
                            Text(
                                text = "${badge.emoji} ${badge.label}".trim(),
                                color = colors.primaryIndigo,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by viewModel.allTasks.collectAsState()
    val userTasks by viewModel.userTasks.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val task = remember(tasks, taskId) { tasks.find { it.id == taskId } }
    val userRecord = remember(userTasks, taskId) { userTasks.find { it.taskId == taskId } }

    var proofNote by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun isPackageInstalled(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        return try {
            context.packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }
    }

    if (task == null) {
        Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(12.dp)) { Icon(Icons.Rounded.ArrowBackIosNew, null) }
            EmptyStateView(Icons.Rounded.SearchOff, "Task Not Found", "This task may have been unpublished or removed.", "Go Back", onBack)
        }
        return
    }

    val installed = remember(task.packageName) { isPackageInstalled(task.packageName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = colors.primaryIndigo) {
                    val prize = if (task.currency == "INR") "₹${task.prizeAmount.toInt()}" else "${task.prizeAmount.toInt()} Coins"
                    Text(prize, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }

        // Centered App Header (Reference Video 01:07)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (task.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = task.iconUrl,
                        contentDescription = task.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(82.dp).clip(RoundedCornerShape(22.dp))
                    )
                } else {
                    IconBadge(Icons.Rounded.Apps, colors.lavenderSoft, colors.primaryIndigo, size = 82.dp, cornerRadius = 22.dp)
                }
                Spacer(Modifier.height(14.dp))
                Text(task.title.ifBlank { task.appName }, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text(task.appName, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                if (task.packageName.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = CircleShape, color = if (installed) colors.mintSoft else colors.amberSoft) {
                        Text(
                            text = if (installed) "App Detected on Device" else "Not Installed Yet",
                            color = if (installed) colors.mintAccent else colors.amberAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Step-by-Step Instructions
        if (task.instructions.isNotEmpty()) {
            item {
                PRCard(cornerRadius = 20.dp) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        task.instructions.forEachIndexed { idx, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape).background(colors.lavenderSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${idx + 1}", color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(step, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Terms & Conditions
        if (task.terms.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.amberSoft,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Terms & Conditions", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Spacer(Modifier.height(4.dp))
                        Text(task.terms, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    }
                }
            }
        }

        // Action Buttons: Visit Play Store + Submit Proof
        item {
            PRCard(cornerRadius = 20.dp) {
                SecondaryOutlineButton(
                    text = "Open App on Play Store",
                    leadingIcon = Icons.Rounded.OpenInNew,
                    onClick = {
                        val url = task.playStoreUrl.ifBlank {
                            if (task.packageName.isNotBlank()) "https://play.google.com/store/apps/details?id=${task.packageName}" else ""
                        }
                        if (url.isNotBlank()) {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                PRTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = "Submission Note / Registered ID (Optional)",
                    placeholder = "Enter registered email or confirmation note"
                )
                Spacer(Modifier.height(14.dp))
                val isResubmit = userRecord?.status == "REJECTED"
                PrimaryGradientButton(
                    text = if (isResubmit) "Resubmit Task (${userRecord?.attemptsUsed ?: 0}/3)" else "Submit Task for Verification",
                    isLoading = submitting,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = {
                        val uid = profile?.uid ?: return@PrimaryGradientButton
                        scope.launch {
                            submitting = true
                            val res = viewModel.repo.startOrSubmitTask(uid, task, proofNote.ifBlank { "Installed & completed steps" }, isResubmit)
                            submitting = false
                            if (res.isSuccess) {
                                val rec = res.getOrThrow()
                                LocalNotificationHelper.scheduleOneDayLeftReminder(context, task.id, task.title, rec.expiresAt)
                                viewModel.showToast("Task submitted! Track approval in Task History.", ToastType.SUCCESS)
                                onBack()
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Submission failed.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TaskHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit
) {
    val colors = PRTheme.colors
    val userTasks by viewModel.userTasks.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Completed", "Rejected")

    val filtered = remember(userTasks, selectedTab) {
        when (selectedTab) {
            0 -> userTasks.filter { it.status == "PENDING" || it.status == "SUBMITTED" }
            1 -> userTasks.filter { it.status == "COMPLETED" }
            else -> userTasks.filter { it.status == "REJECTED" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Column {
                Text("Task History", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text("Track pending, approved, and rejected tasks", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = colors.card) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.History,
                title = "No ${tabs[selectedTab]} Tasks",
                subtitle = when (selectedTab) {
                    0 -> "Started or submitted tasks appear here with a 7-day completion window."
                    1 -> "Tasks approved by the admin and rewarded appear here."
                    else -> "Rejected tasks appear here with up to 3 resubmission attempts."
                }
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.taskId }) { rec ->
                    TaskHistoryCard(rec = rec, onResubmit = { onOpenTask(rec.taskId) })
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(rec: UserTaskRecord, onResubmit: () -> Unit) {
    val colors = PRTheme.colors
    val now = System.currentTimeMillis()
    val daysLeft = if (rec.expiresAt > now) {
        ceil((rec.expiresAt - now).toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
    } else 0

    PRCard(cornerRadius = 18.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rec.title.ifBlank { rec.appName }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(rec.appName, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            val (badgeBg, badgeText, label) = when (rec.status) {
                "COMPLETED" -> Triple(colors.mintSoft, colors.mintAccent, "Approved")
                "REJECTED" -> Triple(colors.roseSoft, colors.roseAccent, "Rejected")
                else -> Triple(colors.amberSoft, colors.amberAccent, "$daysLeft ${if (daysLeft == 1) "day" else "days"} left")
            }
            Surface(shape = CircleShape, color = badgeBg) {
                Text(label, color = badgeText, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }

        if (rec.status == "REJECTED") {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.roseSoft,
                border = BorderStroke(1.dp, colors.roseBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Rejection Reason", color = colors.roseAccent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(rec.rejectionReason.ifBlank { "Verification criteria not met. Please check task steps and resubmit." }, color = colors.roseAccent, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            val canRetry = rec.attemptsUsed < rec.maxAttempts
            if (canRetry) {
                PrimaryGradientButton(
                    text = "Resubmit Task (Attempt ${rec.attemptsUsed}/${rec.maxAttempts})",
                    leadingIcon = Icons.Rounded.Refresh,
                    height = 46.dp,
                    onClick = onResubmit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Maximum 3 attempts used. This entry will auto-remove after 7 days.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
    }
}
