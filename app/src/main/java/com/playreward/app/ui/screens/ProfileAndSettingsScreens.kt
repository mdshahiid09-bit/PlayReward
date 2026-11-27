package com.playreward.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.theme.ThemeMode
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val supportChannels by viewModel.supportChannels.collectAsState()

    var showSupportSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Summary Card with Small Detail Chips + Email Verified Status
        item {
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.heroGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.fullName?.firstOrNull() ?: 'P').uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.fullName?.ifBlank { "PlayReward User" } ?: "PlayReward User", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Text(profile?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        val verified = profile?.emailVerified == true
                        Surface(
                            modifier = Modifier.bounceClick(enabled = !verified && !verifying) {
                                scope.launch {
                                    verifying = true
                                    val r = viewModel.repo.sendVerificationEmail()
                                    verifying = false
                                    if (r.isSuccess) viewModel.showToast("Verification email sent! Tap Refresh after verifying.", ToastType.SUCCESS)
                                    else viewModel.showToast(r.exceptionOrNull()?.message ?: "Failed to send email.", ToastType.ERROR)
                                }
                            },
                            shape = CircleShape,
                            color = if (verified) colors.mintSoft else colors.amberSoft
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (verified) Icons.Rounded.Verified else Icons.Rounded.MarkEmailUnread, null, tint = if (verified) colors.mintAccent else colors.amberAccent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (verified) "Email Verified" else "Verify Email Now",
                                    color = if (verified) colors.mintAccent else Color(0xFFB45309),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val res = viewModel.repo.refreshEmailVerified()
                            if (res.getOrDefault(false)) viewModel.showToast("Email verified!", ToastType.SUCCESS)
                            else viewModel.showToast("Email not verified yet.", ToastType.INFO)
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, "Refresh verification", tint = colors.primaryIndigo)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileDetailPill("Gender", profile?.gender?.ifBlank { "Not set" } ?: "Not set", Modifier.weight(1f))
                    ProfileDetailPill("Age", if ((profile?.age ?: 0) > 0) "${profile?.age} yrs" else "Not set", Modifier.weight(1f))
                }
            }
        }

        // Big Professional Statistics Card
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Account Statistics", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileStatTile("Tasks Completed", "${profile?.totalTasksCompleted ?: 0}", Icons.Rounded.TaskAlt, colors.primaryIndigo, colors.lavenderSoft, Modifier.weight(1f))
                    ProfileStatTile("Total Coins Earned", "${profile?.totalCoinsEarned ?: 0}", Icons.Rounded.MonetizationOn, colors.amberAccent, colors.amberSoft, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileStatTile("Total Withdrawn", "₹${String.format("%.2f", profile?.totalWithdrawnInr ?: 0.0)}", Icons.Rounded.AccountBalanceWallet, colors.mintAccent, colors.mintSoft, Modifier.weight(1f))
                    ProfileStatTile("Total Referrals", "${profile?.totalReferrals ?: 0}", Icons.Rounded.Groups, colors.primaryViolet, colors.lavenderSoft, Modifier.weight(1f))
                }
            }
        }

        // Grouped Settings & Profile Actions Card (Reference Video 00:58)
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                ProfileMenuRow(Icons.Rounded.Edit, "Edit Profile", colors.lavenderSoft, colors.primaryIndigo) { onNavigate("edit_profile") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.Settings, "Settings (Password, Theme, Delete)", colors.amberSoft, colors.amberAccent) { onNavigate("settings") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.SupportAgent, "Contact Support", colors.mintSoft, colors.mintAccent) { showSupportSheet = true }
            }
        }

        // Legal Pages Card
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                ProfileMenuRow(Icons.Rounded.Info, "About PlayReward", colors.inputBg, colors.textSecondary) { onNavigate("legal/about") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.Description, "Terms & Conditions", colors.inputBg, colors.textSecondary) { onNavigate("legal/terms") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.PrivacyTip, "Privacy Policy", colors.inputBg, colors.textSecondary) { onNavigate("legal/privacy") }
            }
        }

        // Standalone Red Sign Out Card (Reference Video 01:03)
        item {
            PRCard(
                onClick = { showLogoutDialog = true },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Logout, null, tint = colors.roseAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = colors.roseAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showSupportSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }, containerColor = colors.card) {
            SupportChannelsContent(channels = supportChannels, onClose = { showSupportSheet = false })
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = colors.card,
            title = { Text("Sign Out?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of PlayReward?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("Sign Out", color = colors.roseAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileDetailPill(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = PRTheme.colors
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.inputBg) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        }
    }
}

@Composable
private fun ProfileStatTile(label: String, value: String, icon: ImageVector, tint: Color, bg: Color, modifier: Modifier = Modifier) {
    val colors = PRTheme.colors
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = colors.inputBg, border = BorderStroke(1.dp, colors.cardBorder)) {
        Column(modifier = Modifier.padding(11.dp)) {
            IconBadge(icon, bg, tint, size = 32.dp, cornerRadius = 10.dp)
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: ImageVector, title: String, badgeBg: Color, iconTint: Color, onClick: () -> Unit) {
    val colors = PRTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, badgeBg, iconTint, size = 38.dp, cornerRadius = 12.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EditProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()

    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var gender by remember(profile?.gender) { mutableStateOf(profile?.gender?.ifBlank { "Male" } ?: "Male") }
    var ageText by remember(profile?.age) { mutableStateOf(if ((profile?.age ?: 0) > 0) profile!!.age.toString() else "") }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        PRCard(cornerRadius = 16.dp) {
            PRTextField(value = profile?.email ?: "", onValueChange = {}, label = "Email ID (Cannot be changed)", enabled = false, leadingIcon = Icons.Rounded.Email)
            Spacer(Modifier.height(12.dp))
            PRTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", leadingIcon = Icons.Rounded.Person)
            Spacer(Modifier.height(12.dp))
            Text("Gender", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Male", "Female", "Other").forEach { g ->
                    FilterChip(selected = gender == g, onClick = { gender = g }, label = { Text(g) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            PRTextField(
                value = ageText,
                onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                label = "Age",
                leadingIcon = Icons.Rounded.Cake,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(18.dp))
            PrimaryGradientButton(
                text = "Save Profile",
                isLoading = saving,
                enabled = fullName.isNotBlank(),
                onClick = {
                    val uid = profile?.uid ?: return@PrimaryGradientButton
                    val age = ageText.toIntOrNull() ?: 0
                    scope.launch {
                        saving = true
                        val res = viewModel.repo.updateProfile(uid, fullName, gender, age)
                        saving = false
                        if (res.isSuccess) {
                            viewModel.showToast("Profile updated!", ToastType.SUCCESS)
                            onBack()
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Update failed.", ToastType.ERROR)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var changingPass by remember { mutableStateOf(false) }

    var deletePass by remember { mutableStateOf("") }
    var deleteReason by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val deleteReq = profile?.deleteRequest
    val daysToPermanentDelete = if (deleteReq?.requested == true && deleteReq.scheduledDeleteAt > System.currentTimeMillis()) {
        ceil((deleteReq.scheduledDeleteAt - System.currentTimeMillis()).toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }

        // Theme Mode (Light / Dark / System)
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System").forEach { (m, label) ->
                        FilterChip(
                            selected = themeMode == m,
                            onClick = { viewModel.setTheme(m) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Change Password
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Change Password", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(12.dp))
                PRTextField(value = oldPass, onValueChange = { oldPass = it }, label = "Current Password", visualTransformation = PasswordVisualTransformation(), leadingIcon = Icons.Rounded.Lock)
                Spacer(Modifier.height(10.dp))
                PRTextField(value = newPass, onValueChange = { newPass = it }, label = "New Password", visualTransformation = PasswordVisualTransformation(), leadingIcon = Icons.Rounded.LockReset)
                Spacer(Modifier.height(10.dp))
                PRTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = "Confirm New Password", visualTransformation = PasswordVisualTransformation(), leadingIcon = Icons.Rounded.LockReset)
                Spacer(Modifier.height(14.dp))
                PrimaryGradientButton(
                    text = "Save Password",
                    isLoading = changingPass,
                    enabled = oldPass.isNotBlank() && newPass.length >= 6 && newPass == confirmPass,
                    onClick = {
                        scope.launch {
                            changingPass = true
                            val res = viewModel.repo.changePassword(oldPass, newPass)
                            changingPass = false
                            if (res.isSuccess) {
                                oldPass = ""; newPass = ""; confirmPass = ""
                                viewModel.showToast("Password updated successfully!", ToastType.SUCCESS)
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Password update failed.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Delete Account (7-Day Grace Period)
        item {
            PRCard(cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.DeleteForever, colors.roseSoft, colors.roseAccent, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Delete Account", style = MaterialTheme.typography.titleMedium, color = colors.roseAccent)
                        Text("7-day recovery window before permanent removal", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (deleteReq?.requested == true) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.roseSoft,
                        border = BorderStroke(1.dp, colors.roseBorder)
                    ) {
                        Column(modifier = Modifier.padding(11.dp)) {
                            Text("Account Scheduled for Deletion", fontWeight = FontWeight.Bold, color = colors.roseAccent)
                            Spacer(Modifier.height(4.dp))
                            Text("$daysToPermanentDelete days left to recover your account before all data is permanently deleted.", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryOutlineButton(
                        text = "Cancel Deletion & Recover Account",
                        onClick = {
                            val uid = profile?.uid ?: return@SecondaryOutlineButton
                            scope.launch {
                                viewModel.repo.cancelAccountDeletion(uid)
                                viewModel.showToast("Account deletion cancelled!", ToastType.SUCCESS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Requesting permanent deletion schedules your profile, coins, and history for removal after 7 days. You can cancel anytime within those 7 days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    PRTextField(value = deleteReason, onValueChange = { deleteReason = it }, label = "Reason (Optional)")
                    Spacer(Modifier.height(10.dp))
                    PRTextField(value = deletePass, onValueChange = { deletePass = it }, label = "Enter Password to Confirm", visualTransformation = PasswordVisualTransformation(), leadingIcon = Icons.Rounded.Lock)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = !deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.roseAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Request Permanent Deletion", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.card,
            title = { Text("Confirm Account Deletion", color = colors.roseAccent, fontWeight = FontWeight.Bold) },
            text = { Text("Your account will be scheduled for permanent deletion in 7 days. You can recover it within 7 days.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        deleting = true
                        val res = viewModel.repo.requestAccountDeletion(deletePass, deleteReason)
                        deleting = false
                        if (res.isSuccess) {
                            deletePass = ""
                            viewModel.showToast("Account scheduled for deletion (7 days left to recover).", ToastType.INFO)
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Re-authentication failed.", ToastType.ERROR)
                        }
                    }
                }) { Text("Confirm Delete", color = colors.roseAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LegalPageScreen(pageType: String, viewModel: MainViewModel, onBack: () -> Unit) {
    val colors = PRTheme.colors
    val legal by viewModel.legalConfig.collectAsState()
    val (title, body) = when (pageType) {
        "about" -> "About PlayReward" to legal.about.ifBlank { "PlayReward is a rewards app for verified tasks, quizzes, and skill games." }
        "terms" -> "Terms & Conditions" to legal.terms.ifBlank { "Terms & Conditions managed from the admin panel will appear here." }
        else -> "Privacy Policy" to legal.privacy.ifBlank { "Privacy Policy managed from the admin panel will appear here." }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }
        item {
            PRCard(cornerRadius = 16.dp) {
                Text(body, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
        }
    }
}
