package com.playreward.app.data.model

data class AppConfig(
    val appName: String = "PlayReward",
    val maintenanceMode: Boolean = false,
    val maintenanceTitle: String = "Under Maintenance",
    val maintenanceMessage: String = "We are upgrading our servers. Please check back soon.",
    val adminBypassUids: Map<String, Boolean> = emptyMap(),
    val minWithdrawalCoins: Long = 10000L,
    val maxWithdrawalCoins: Long = 100000L,
    val coinsPerInr: Long = 1000L,
    val minConvertCoins: Long = 10000L,
    val tasksHeaderText: String = "Complete tasks and earn coins up to 5000",
    val githubOwner: String = "mdshahiid09-bit",
    val githubRepo: String = "PlayReward",
    val forceUpdateEnabled: Boolean = true
)

data class ReferralConfig(
    val enabled: Boolean = true,
    val referrerRewardCoins: Long = 3000L,
    val signupBonusCoins: Long = 500L,
    val referralLinkEnabled: Boolean = true,
    val referralBaseUrl: String = "https://play.google.com/store/apps/details?id=com.playreward.app",
    val shareGreetingMessage: String = "Join PlayReward with my invite code {CODE} and earn instant bonus rewards!"
)

data class StartupPopupConfig(
    val enabled: Boolean = false,
    val imageUrl: String = "",
    val actionUrl: String = "",
    val closeDelaySeconds: Int = 3
)

data class OnboardingPage(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val order: Int = 0
)

data class BannerItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val actionType: String = "NONE",
    val actionValue: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

data class LegalConfig(
    val about: String = "",
    val terms: String = "",
    val privacy: String = ""
)

data class SupportChannel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val profileUrl: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

data class QuizSettings(
    val enabled: Boolean = true,
    val websiteUrl: String = "",
    val requiredExploreSeconds: Int = 15,
    val gemGoal: Int = 10,
    val rewardCoins: Long = 800L,
    val gemsPerCorrect: Int = 1
)

data class QuizCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

data class QuizQuestion(
    val id: String = "",
    val categoryId: String = "",
    val question: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctIndex: Int = 0,
    val hint: String = "",
    val active: Boolean = true
)

data class MegaOfferConfig(
    val enabled: Boolean = true,
    val title: String = "Mega Offer",
    val subtitle: String = "Match the exact number to collect gems and unlock coin rewards.",
    val websiteUrl: String = "",
    val requiredExploreSeconds: Int = 15,
    val correctAnswersNeeded: Int = 3,
    val gemsPerWin: Int = 1,
    val gemGoal: Int = 10,
    val rewardCoins: Long = 800L
)

data class TicTacToeTier(
    val id: String = "",
    val difficulty: String = "LOW", // LOW, MEDIUM, HIGH
    val entryCoins: Long = 10L,
    val winCoins: Long = 12L,
    val label: String = "Starter"
)

data class SuperOfferConfig(
    val enabled: Boolean = true,
    val title: String = "Super Offer — Tic Tac Toe Arena",
    val termsAndConditions: String = "",
    val tiers: List<TicTacToeTier> = emptyList()
)

data class TaskBadge(
    val label: String = "",
    val emoji: String = "",
    val colorHex: String = "#5B6AF0"
)

data class TaskCategory(
    val id: String = "",
    val name: String = "",
    val active: Boolean = true,
    val order: Int = 0
)

data class AppTask(
    val id: String = "",
    val appName: String = "",
    val title: String = "",
    val playStoreUrl: String = "",
    val packageName: String = "",
    val iconUrl: String = "",
    val prizeAmount: Double = 0.0,
    val currency: String = "COIN", // COIN or INR
    val instructions: List<String> = emptyList(),
    val terms: String = "",
    val badges: List<TaskBadge> = emptyList(),
    val categoryId: String = "",
    val status: String = "PUBLISHED", // PUBLISHED or DRAFT
    val visible: Boolean = true,
    val enabled: Boolean = true,
    val featured: Boolean = false,
    val createdAt: Long = 0L
)

data class UserTaskRecord(
    val taskId: String = "",
    val appName: String = "",
    val title: String = "",
    val prizeAmount: Double = 0.0,
    val currency: String = "COIN",
    val status: String = "PENDING", // PENDING, SUBMITTED, COMPLETED, REJECTED
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 3,
    val proofNote: String = "",
    val rejectionReason: String = "",
    val startedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class PayoutMethods(
    val upiId: String = "",
    val bankAccountHolder: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val bankPhone: String = ""
)

data class DeleteRequest(
    val requested: Boolean = false,
    val requestedAt: Long = 0L,
    val scheduledDeleteAt: Long = 0L,
    val reason: String = ""
)

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val gender: String = "",
    val age: Int = 0,
    val avatarUrl: String = "",
    val emailVerified: Boolean = false,
    val coins: Long = 0L,
    val inrBalance: Double = 0.0,
    val totalCoinsEarned: Long = 0L,
    val totalWithdrawnInr: Double = 0.0,
    val totalTasksCompleted: Int = 0,
    val totalReferrals: Int = 0,
    val completedReferrals: Int = 0,
    val pendingReferrals: Int = 0,
    val totalReferralEarnedInr: Double = 0.0,
    val referralCode: String = "",
    val referredByUid: String = "",
    val referredByCode: String = "",
    val firstWithdrawalApproved: Boolean = false,
    val referralPromptDone: Boolean = false,
    val quizGems: Int = 0,
    val megaOfferGems: Int = 0,
    val createdAt: Long = 0L,
    val payoutMethods: PayoutMethods = PayoutMethods(),
    val deleteRequest: DeleteRequest = DeleteRequest()
)

data class ReferralRecord(
    val referredUid: String = "",
    val referredName: String = "",
    val referredEmail: String = "",
    val status: String = "PENDING", // PENDING or COMPLETED
    val rewardCoins: Long = 0L,
    val rewardInr: Double = 0.0,
    val createdAt: Long = 0L,
    val completedAt: Long = 0L
)

data class TransactionItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val amountCoins: Long = 0L,
    val amountInr: Double = 0.0,
    val status: String = "COMPLETED",
    val createdAt: Long = 0L
)

data class LeaderboardEntry(
    val uid: String = "",
    val fullName: String = "",
    val totalCoinsEarned: Long = 0L,
    val avatarUrl: String = ""
)

data class AppNotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "INFO",
    val read: Boolean = false,
    val createdAt: Long = 0L
)

data class GithubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String
)
