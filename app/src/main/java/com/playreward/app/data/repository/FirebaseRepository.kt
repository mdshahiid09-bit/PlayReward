package com.playreward.app.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.playreward.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class FirebaseRepository {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseDatabase = FirebaseDatabase.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    private fun <T> observeNode(path: String, mapper: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val ref = db.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(mapper(snapshot))
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAppConfig(): Flow<AppConfig> = observeNode("config/app") { snap ->
        val bypassMap = mutableMapOf<String, Boolean>()
        snap.child("adminBypassUids").children.forEach { child ->
            val k = child.key ?: return@forEach
            if (child.getValue(Boolean::class.java) == true) bypassMap[k] = true
        }
        AppConfig(
            appName = snap.child("appName").getValue(String::class.java) ?: "PlayReward",
            maintenanceMode = snap.child("maintenanceMode").getValue(Boolean::class.java) ?: false,
            maintenanceTitle = snap.child("maintenanceTitle").getValue(String::class.java) ?: "Scheduled Maintenance",
            maintenanceMessage = snap.child("maintenanceMessage").getValue(String::class.java) ?: "We are upgrading PlayReward. Please check back soon.",
            adminBypassUids = bypassMap,
            minWithdrawalCoins = snap.child("minWithdrawalCoins").getValue(Long::class.java) ?: 10000L,
            maxWithdrawalCoins = snap.child("maxWithdrawalCoins").getValue(Long::class.java) ?: 100000L,
            coinsPerInr = snap.child("coinsPerInr").getValue(Long::class.java) ?: 1000L,
            minConvertCoins = snap.child("minConvertCoins").getValue(Long::class.java) ?: 10000L,
            tasksHeaderText = snap.child("tasksHeaderText").getValue(String::class.java) ?: "Complete tasks and earn coins up to 5000",
            githubOwner = snap.child("githubOwner").getValue(String::class.java) ?: "mdshahiid09-bit",
            githubRepo = snap.child("githubRepo").getValue(String::class.java) ?: "PlayReward",
            forceUpdateEnabled = snap.child("forceUpdateEnabled").getValue(Boolean::class.java) ?: true
        )
    }

    fun observeReferralConfig(): Flow<ReferralConfig> = observeNode("config/referral") { snap ->
        ReferralConfig(
            enabled = snap.child("enabled").getValue(Boolean::class.java) ?: true,
            referrerRewardCoins = snap.child("referrerRewardCoins").getValue(Long::class.java) ?: 3000L,
            signupBonusCoins = snap.child("signupBonusCoins").getValue(Long::class.java) ?: 500L,
            referralLinkEnabled = snap.child("referralLinkEnabled").getValue(Boolean::class.java) ?: true,
            referralBaseUrl = snap.child("referralBaseUrl").getValue(String::class.java) ?: "",
            shareGreetingMessage = snap.child("shareGreetingMessage").getValue(String::class.java)
                ?: "Join PlayReward with my code {CODE} and earn rewards!"
        )
    }

    fun observeStartupPopup(): Flow<StartupPopupConfig> = observeNode("config/startupPopup") { snap ->
        StartupPopupConfig(
            enabled = snap.child("enabled").getValue(Boolean::class.java) ?: false,
            imageUrl = snap.child("imageUrl").getValue(String::class.java) ?: "",
            actionUrl = snap.child("actionUrl").getValue(String::class.java) ?: "",
            closeDelaySeconds = (snap.child("closeDelaySeconds").getValue(Long::class.java) ?: 3L).toInt().coerceAtLeast(1)
        )
    }

    fun observeOnboardingPages(): Flow<List<OnboardingPage>> = observeNode("config/onboarding") { snap ->
        snap.children.mapNotNull { c ->
            val title = c.child("title").getValue(String::class.java) ?: return@mapNotNull null
            OnboardingPage(
                id = c.key ?: "",
                title = title,
                subtitle = c.child("subtitle").getValue(String::class.java) ?: "",
                imageUrl = c.child("imageUrl").getValue(String::class.java) ?: "",
                order = (c.child("order").getValue(Long::class.java) ?: 0L).toInt()
            )
        }.sortedBy { it.order }
    }

    fun observeBanners(): Flow<List<BannerItem>> = observeNode("config/banners") { snap ->
        snap.children.mapNotNull { c ->
            val active = c.child("active").getValue(Boolean::class.java) ?: true
            if (!active) return@mapNotNull null
            BannerItem(
                id = c.key ?: "",
                title = c.child("title").getValue(String::class.java) ?: "",
                subtitle = c.child("subtitle").getValue(String::class.java) ?: "",
                imageUrl = c.child("imageUrl").getValue(String::class.java) ?: "",
                actionType = c.child("actionType").getValue(String::class.java) ?: "NONE",
                actionValue = c.child("actionValue").getValue(String::class.java) ?: "",
                active = true,
                order = (c.child("order").getValue(Long::class.java) ?: 0L).toInt()
            )
        }.sortedBy { it.order }
    }

    fun observeLegalConfig(): Flow<LegalConfig> = observeNode("config/legal") { snap ->
        LegalConfig(
            about = snap.child("about").getValue(String::class.java) ?: "",
            terms = snap.child("terms").getValue(String::class.java) ?: "",
            privacy = snap.child("privacy").getValue(String::class.java) ?: ""
        )
    }

    fun observeSupportChannels(): Flow<List<SupportChannel>> = observeNode("config/support") { snap ->
        snap.children.mapNotNull { c ->
            val active = c.child("active").getValue(Boolean::class.java) ?: true
            if (!active) return@mapNotNull null
            val name = c.child("name").getValue(String::class.java) ?: return@mapNotNull null
            SupportChannel(
                id = c.key ?: "",
                name = name,
                logoUrl = c.child("logoUrl").getValue(String::class.java) ?: "",
                profileUrl = c.child("profileUrl").getValue(String::class.java) ?: "",
                active = true,
                order = (c.child("order").getValue(Long::class.java) ?: 0L).toInt()
            )
        }.sortedBy { it.order }
    }

    fun observeQuizSettings(): Flow<QuizSettings> = observeNode("config/quizSettings") { snap ->
        QuizSettings(
            enabled = snap.child("enabled").getValue(Boolean::class.java) ?: true,
            websiteUrl = snap.child("websiteUrl").getValue(String::class.java) ?: "",
            requiredExploreSeconds = (snap.child("requiredExploreSeconds").getValue(Long::class.java) ?: 15L).toInt(),
            gemGoal = (snap.child("gemGoal").getValue(Long::class.java) ?: 10L).toInt().coerceAtLeast(1),
            rewardCoins = snap.child("rewardCoins").getValue(Long::class.java) ?: 800L,
            gemsPerCorrect = (snap.child("gemsPerCorrect").getValue(Long::class.java) ?: 1L).toInt().coerceAtLeast(1)
        )
    }

    fun observeQuizCategories(): Flow<List<QuizCategory>> = observeNode("quizCategories") { snap ->
        snap.children.mapNotNull { c ->
            val active = c.child("active").getValue(Boolean::class.java) ?: true
            if (!active) return@mapNotNull null
            QuizCategory(
                id = c.key ?: return@mapNotNull null,
                name = c.child("name").getValue(String::class.java) ?: "",
                description = c.child("description").getValue(String::class.java) ?: "",
                iconUrl = c.child("iconUrl").getValue(String::class.java) ?: "",
                active = true,
                order = (c.child("order").getValue(Long::class.java) ?: 0L).toInt()
            )
        }.sortedBy { it.order }
    }

    fun observeQuizzesForCategory(categoryId: String): Flow<List<QuizQuestion>> = observeNode("quizzes/$categoryId") { snap ->
        snap.children.mapNotNull { c ->
            val active = c.child("active").getValue(Boolean::class.java) ?: true
            if (!active) return@mapNotNull null
            QuizQuestion(
                id = c.key ?: return@mapNotNull null,
                categoryId = categoryId,
                question = c.child("question").getValue(String::class.java) ?: "",
                optionA = c.child("optionA").getValue(String::class.java) ?: "",
                optionB = c.child("optionB").getValue(String::class.java) ?: "",
                optionC = c.child("optionC").getValue(String::class.java) ?: "",
                optionD = c.child("optionD").getValue(String::class.java) ?: "",
                correctIndex = (c.child("correctIndex").getValue(Long::class.java) ?: 0L).toInt(),
                hint = c.child("hint").getValue(String::class.java) ?: "",
                active = true
            )
        }
    }

    fun observeMegaOfferConfig(): Flow<MegaOfferConfig> = observeNode("config/megaOffer") { snap ->
        MegaOfferConfig(
            enabled = snap.child("enabled").getValue(Boolean::class.java) ?: true,
            title = snap.child("title").getValue(String::class.java) ?: "Mega Offer",
            subtitle = snap.child("subtitle").getValue(String::class.java) ?: "Match the exact number to collect gems.",
            websiteUrl = snap.child("websiteUrl").getValue(String::class.java) ?: "",
            requiredExploreSeconds = (snap.child("requiredExploreSeconds").getValue(Long::class.java) ?: 15L).toInt(),
            correctAnswersNeeded = (snap.child("correctAnswersNeeded").getValue(Long::class.java) ?: 3L).toInt().coerceIn(1, 5),
            gemsPerWin = (snap.child("gemsPerWin").getValue(Long::class.java) ?: 1L).toInt().coerceAtLeast(1),
            gemGoal = (snap.child("gemGoal").getValue(Long::class.java) ?: 10L).toInt().coerceAtLeast(1),
            rewardCoins = snap.child("rewardCoins").getValue(Long::class.java) ?: 800L
        )
    }

    fun observeSuperOfferConfig(): Flow<SuperOfferConfig> = observeNode("config/superOffer") { snap ->
        val tiers = snap.child("tiers").children.mapNotNull { t ->
            TicTacToeTier(
                id = t.key ?: return@mapNotNull null,
                difficulty = (t.child("difficulty").getValue(String::class.java) ?: "LOW").uppercase(Locale.US),
                entryCoins = t.child("entryCoins").getValue(Long::class.java) ?: 10L,
                winCoins = t.child("winCoins").getValue(Long::class.java) ?: 12L,
                label = t.child("label").getValue(String::class.java) ?: "Arena"
            )
        }.sortedWith(compareBy<TicTacToeTier> {
            when (it.difficulty) {
                "LOW" -> 0
                "MEDIUM" -> 1
                else -> 2
            }
        }.thenBy { it.entryCoins })

        SuperOfferConfig(
            enabled = snap.child("enabled").getValue(Boolean::class.java) ?: true,
            title = snap.child("title").getValue(String::class.java) ?: "Super Offer — Tic Tac Toe Arena",
            termsAndConditions = snap.child("termsAndConditions").getValue(String::class.java) ?: "",
            tiers = tiers
        )
    }

    fun observeTaskCategories(): Flow<List<TaskCategory>> = observeNode("taskCategories") { snap ->
        snap.children.mapNotNull { c ->
            val active = c.child("active").getValue(Boolean::class.java) ?: true
            if (!active) return@mapNotNull null
            TaskCategory(
                id = c.key ?: return@mapNotNull null,
                name = c.child("name").getValue(String::class.java) ?: "",
                active = true,
                order = (c.child("order").getValue(Long::class.java) ?: 0L).toInt()
            )
        }.sortedBy { it.order }
    }

    fun observeTasks(): Flow<List<AppTask>> = observeNode("tasks") { snap ->
        snap.children.mapNotNull { c ->
            val status = c.child("status").getValue(String::class.java) ?: "PUBLISHED"
            val visible = c.child("visible").getValue(Boolean::class.java) ?: true
            val enabled = c.child("enabled").getValue(Boolean::class.java) ?: true
            if (status != "PUBLISHED" || !visible || !enabled) return@mapNotNull null

            val instructions = c.child("instructions").children.mapNotNull { it.getValue(String::class.java) }
            val badges = c.child("badges").children.mapNotNull { b ->
                val label = b.child("label").getValue(String::class.java) ?: return@mapNotNull null
                TaskBadge(
                    label = label,
                    emoji = b.child("emoji").getValue(String::class.java) ?: "",
                    colorHex = b.child("colorHex").getValue(String::class.java) ?: "#5B6AF0"
                )
            }
            AppTask(
                id = c.key ?: return@mapNotNull null,
                appName = c.child("appName").getValue(String::class.java) ?: "",
                title = c.child("title").getValue(String::class.java) ?: "",
                playStoreUrl = c.child("playStoreUrl").getValue(String::class.java) ?: "",
                packageName = c.child("packageName").getValue(String::class.java) ?: "",
                iconUrl = c.child("iconUrl").getValue(String::class.java) ?: "",
                prizeAmount = c.child("prizeAmount").getValue(Double::class.java)
                    ?: (c.child("prizeAmount").getValue(Long::class.java)?.toDouble() ?: 0.0),
                currency = (c.child("currency").getValue(String::class.java) ?: "COIN").uppercase(Locale.US),
                instructions = instructions,
                terms = c.child("terms").getValue(String::class.java) ?: "",
                badges = badges,
                categoryId = c.child("categoryId").getValue(String::class.java) ?: "",
                status = status,
                visible = visible,
                enabled = enabled,
                featured = c.child("featured").getValue(Boolean::class.java) ?: false,
                createdAt = c.child("createdAt").getValue(Long::class.java) ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = observeNode("users/$uid") { snap ->
        if (!snap.exists()) return@observeNode null
        val pm = snap.child("payoutMethods")
        val dr = snap.child("deleteRequest")
        UserProfile(
            uid = uid,
            fullName = snap.child("fullName").getValue(String::class.java) ?: "",
            email = snap.child("email").getValue(String::class.java) ?: "",
            gender = snap.child("gender").getValue(String::class.java) ?: "",
            age = (snap.child("age").getValue(Long::class.java) ?: 0L).toInt(),
            avatarUrl = snap.child("avatarUrl").getValue(String::class.java) ?: "",
            emailVerified = auth.currentUser?.isEmailVerified ?: (snap.child("emailVerified").getValue(Boolean::class.java) ?: false),
            coins = snap.child("coins").getValue(Long::class.java) ?: 0L,
            inrBalance = snap.child("inrBalance").getValue(Double::class.java)
                ?: (snap.child("inrBalance").getValue(Long::class.java)?.toDouble() ?: 0.0),
            totalCoinsEarned = snap.child("totalCoinsEarned").getValue(Long::class.java) ?: 0L,
            totalWithdrawnInr = snap.child("totalWithdrawnInr").getValue(Double::class.java) ?: 0.0,
            totalTasksCompleted = (snap.child("totalTasksCompleted").getValue(Long::class.java) ?: 0L).toInt(),
            totalReferrals = (snap.child("totalReferrals").getValue(Long::class.java) ?: 0L).toInt(),
            completedReferrals = (snap.child("completedReferrals").getValue(Long::class.java) ?: 0L).toInt(),
            pendingReferrals = (snap.child("pendingReferrals").getValue(Long::class.java) ?: 0L).toInt(),
            totalReferralEarnedInr = snap.child("totalReferralEarnedInr").getValue(Double::class.java) ?: 0.0,
            referralCode = snap.child("referralCode").getValue(String::class.java) ?: "",
            referredByUid = snap.child("referredByUid").getValue(String::class.java) ?: "",
            referredByCode = snap.child("referredByCode").getValue(String::class.java) ?: "",
            firstWithdrawalApproved = snap.child("firstWithdrawalApproved").getValue(Boolean::class.java) ?: false,
            referralPromptDone = snap.child("referralPromptDone").getValue(Boolean::class.java) ?: false,
            quizGems = (snap.child("quizGems").getValue(Long::class.java) ?: 0L).toInt(),
            megaOfferGems = (snap.child("megaOfferGems").getValue(Long::class.java) ?: 0L).toInt(),
            createdAt = snap.child("createdAt").getValue(Long::class.java) ?: 0L,
            payoutMethods = PayoutMethods(
                upiId = pm.child("upiId").getValue(String::class.java) ?: "",
                bankAccountHolder = pm.child("bankAccountHolder").getValue(String::class.java) ?: "",
                bankName = pm.child("bankName").getValue(String::class.java) ?: "",
                bankAccountNumber = pm.child("bankAccountNumber").getValue(String::class.java) ?: "",
                bankIfsc = pm.child("bankIfsc").getValue(String::class.java) ?: "",
                bankPhone = pm.child("bankPhone").getValue(String::class.java) ?: ""
            ),
            deleteRequest = DeleteRequest(
                requested = dr.child("requested").getValue(Boolean::class.java) ?: false,
                requestedAt = dr.child("requestedAt").getValue(Long::class.java) ?: 0L,
                scheduledDeleteAt = dr.child("scheduledDeleteAt").getValue(Long::class.java) ?: 0L,
                reason = dr.child("reason").getValue(String::class.java) ?: ""
            )
        )
    }

    fun observeUserTasks(uid: String): Flow<List<UserTaskRecord>> = observeNode("userTasks/$uid") { snap ->
        snap.children.mapNotNull { c ->
            UserTaskRecord(
                taskId = c.key ?: return@mapNotNull null,
                appName = c.child("appName").getValue(String::class.java) ?: "",
                title = c.child("title").getValue(String::class.java) ?: "",
                prizeAmount = c.child("prizeAmount").getValue(Double::class.java)
                    ?: (c.child("prizeAmount").getValue(Long::class.java)?.toDouble() ?: 0.0),
                currency = c.child("currency").getValue(String::class.java) ?: "COIN",
                status = c.child("status").getValue(String::class.java) ?: "PENDING",
                attemptsUsed = (c.child("attemptsUsed").getValue(Long::class.java) ?: 0L).toInt(),
                maxAttempts = (c.child("maxAttempts").getValue(Long::class.java) ?: 3L).toInt(),
                proofNote = c.child("proofNote").getValue(String::class.java) ?: "",
                rejectionReason = c.child("rejectionReason").getValue(String::class.java) ?: "",
                startedAt = c.child("startedAt").getValue(Long::class.java) ?: 0L,
                expiresAt = c.child("expiresAt").getValue(Long::class.java) ?: 0L,
                updatedAt = c.child("updatedAt").getValue(Long::class.java) ?: 0L
            )
        }.sortedByDescending { it.updatedAt }
    }

    fun observeReferrals(uid: String): Flow<List<ReferralRecord>> = observeNode("referrals/$uid") { snap ->
        snap.children.mapNotNull { c ->
            ReferralRecord(
                referredUid = c.key ?: return@mapNotNull null,
                referredName = c.child("referredName").getValue(String::class.java) ?: "User",
                referredEmail = c.child("referredEmail").getValue(String::class.java) ?: "",
                status = c.child("status").getValue(String::class.java) ?: "PENDING",
                rewardCoins = c.child("rewardCoins").getValue(Long::class.java) ?: 0L,
                rewardInr = c.child("rewardInr").getValue(Double::class.java) ?: 0.0,
                createdAt = c.child("createdAt").getValue(Long::class.java) ?: 0L,
                completedAt = c.child("completedAt").getValue(Long::class.java) ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    fun observeTransactions(uid: String): Flow<List<TransactionItem>> = observeNode("transactions/$uid") { snap ->
        snap.children.mapNotNull { c ->
            TransactionItem(
                id = c.key ?: return@mapNotNull null,
                type = c.child("type").getValue(String::class.java) ?: "",
                title = c.child("title").getValue(String::class.java) ?: "",
                amountCoins = c.child("amountCoins").getValue(Long::class.java) ?: 0L,
                amountInr = c.child("amountInr").getValue(Double::class.java) ?: 0.0,
                status = c.child("status").getValue(String::class.java) ?: "COMPLETED",
                createdAt = c.child("createdAt").getValue(Long::class.java) ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = observeNode("leaderboard") { snap ->
        snap.children.mapNotNull { c ->
            LeaderboardEntry(
                uid = c.key ?: return@mapNotNull null,
                fullName = c.child("fullName").getValue(String::class.java) ?: "Player",
                totalCoinsEarned = c.child("totalCoinsEarned").getValue(Long::class.java) ?: 0L,
                avatarUrl = c.child("avatarUrl").getValue(String::class.java) ?: ""
            )
        }.sortedByDescending { it.totalCoinsEarned }.take(50)
    }

    fun observeNotifications(uid: String): Flow<List<AppNotificationItem>> = observeNode("notifications/$uid") { snap ->
        snap.children.mapNotNull { c ->
            AppNotificationItem(
                id = c.key ?: return@mapNotNull null,
                title = c.child("title").getValue(String::class.java) ?: "",
                body = c.child("body").getValue(String::class.java) ?: "",
                type = c.child("type").getValue(String::class.java) ?: "INFO",
                read = c.child("read").getValue(Boolean::class.java) ?: false,
                createdAt = c.child("createdAt").getValue(Long::class.java) ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    // --- Authentication ---

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user ?: error("Authentication failed.")
    }

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        gender: String,
        age: Int,
        password: String,
        referralCodeInput: String,
        referralConfig: ReferralConfig,
        coinsPerInr: Long
    ): Result<FirebaseUser> = runCatching {
        val cleanCode = referralCodeInput.trim().uppercase(Locale.US)
        var referrerUid = ""
        if (cleanCode.isNotEmpty() && referralConfig.enabled) {
            val refSnap = db.getReference("referralCodes/$cleanCode").get().await()
            referrerUid = refSnap.getValue(String::class.java) ?: ""
            if (referrerUid.isEmpty()) error("Invalid referral code.")
        }

        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("Account creation failed.")
        user.sendEmailVerification()

        initializeUserRecord(
            user = user,
            fullName = fullName.trim(),
            gender = gender,
            age = age,
            referrerUid = referrerUid,
            referrerCode = if (referrerUid.isNotEmpty()) cleanCode else "",
            referralConfig = referralConfig,
            coinsPerInr = coinsPerInr
        )
        user
    }

    suspend fun signInWithGoogle(
        idToken: String,
        referralConfig: ReferralConfig,
        coinsPerInr: Long
    ): Result<FirebaseUser> = runCatching {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val res = auth.signInWithCredential(cred).await()
        val user = res.user ?: error("Google sign-in failed.")
        val existing = db.getReference("users/${user.uid}").get().await()
        if (!existing.exists()) {
            initializeUserRecord(
                user = user,
                fullName = user.displayName ?: "PlayReward User",
                gender = "",
                age = 0,
                referrerUid = "",
                referrerCode = "",
                referralConfig = referralConfig,
                coinsPerInr = coinsPerInr
            )
        }
        user
    }

    private suspend fun initializeUserRecord(
        user: FirebaseUser,
        fullName: String,
        gender: String,
        age: Int,
        referrerUid: String,
        referrerCode: String,
        referralConfig: ReferralConfig,
        coinsPerInr: Long
    ) {
        val now = System.currentTimeMillis()
        val myCode = "PR" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        val bonusCoins = if (referrerUid.isNotEmpty() && referralConfig.enabled) referralConfig.signupBonusCoins else 0L

        val userMap = mapOf(
            "uid" to user.uid,
            "fullName" to fullName,
            "email" to (user.email ?: ""),
            "gender" to gender,
            "age" to age,
            "avatarUrl" to (user.photoUrl?.toString() ?: ""),
            "emailVerified" to user.isEmailVerified,
            "coins" to bonusCoins,
            "inrBalance" to 0.0,
            "totalCoinsEarned" to bonusCoins,
            "totalWithdrawnInr" to 0.0,
            "totalTasksCompleted" to 0,
            "totalReferrals" to 0,
            "completedReferrals" to 0,
            "pendingReferrals" to 0,
            "totalReferralEarnedInr" to 0.0,
            "referralCode" to myCode,
            "referredByUid" to referrerUid,
            "referredByCode" to referrerCode,
            "firstWithdrawalApproved" to false,
            "referralPromptDone" to (age > 0 || referrerUid.isNotEmpty()),
            "quizGems" to 0,
            "megaOfferGems" to 0,
            "createdAt" to now
        )
        db.getReference("users/${user.uid}").setValue(userMap).await()
        db.getReference("referralCodes/$myCode").setValue(user.uid).await()
        db.getReference("leaderboard/${user.uid}").setValue(
            mapOf(
                "uid" to user.uid,
                "fullName" to fullName,
                "totalCoinsEarned" to bonusCoins,
                "avatarUrl" to (user.photoUrl?.toString() ?: "")
            )
        ).await()

        if (bonusCoins > 0) {
            logTransaction(
                uid = user.uid,
                type = "SIGNUP_BONUS",
                title = "Referral Signup Bonus",
                amountCoins = bonusCoins,
                amountInr = 0.0
            )
        }

        if (referrerUid.isNotEmpty() && referrerUid != user.uid && referralConfig.enabled) {
            val rewardInr = if (coinsPerInr > 0) referralConfig.referrerRewardCoins.toDouble() / coinsPerInr else 0.0
            db.getReference("referrals/$referrerUid/${user.uid}").setValue(
                mapOf(
                    "referredName" to fullName,
                    "referredEmail" to (user.email ?: ""),
                    "status" to "PENDING",
                    "rewardCoins" to referralConfig.referrerRewardCoins,
                    "rewardInr" to rewardInr,
                    "createdAt" to now,
                    "completedAt" to 0L
                )
            ).await()
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun sendVerificationEmail(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.sendEmailVerification().await()
    }

    suspend fun refreshEmailVerified(): Result<Boolean> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.reload().await()
        val verified = user.isEmailVerified
        db.getReference("users/${user.uid}/emailVerified").setValue(verified).await()
        verified
    }

    suspend fun saveAdminBypassUid(uid: String): Result<Unit> = runCatching {
        val clean = uid.trim()
        require(clean.isNotEmpty()) { "Please enter a valid UID." }
        db.getReference("config/app/adminBypassUids/$clean").setValue(true).await()
    }

    // --- Atomic Coin & Gem Operations ---

    suspend fun adjustCoins(uid: String, deltaCoins: Long, countAsEarned: Boolean, txType: String, txTitle: String): Result<Long> = runCatching {
        val newBalance = suspendCancellableCoroutine<Long> { cont ->
            val ref = db.getReference("users/$uid")
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.child("coins").getValue(Long::class.java) ?: 0L
                    val updated = current + deltaCoins
                    if (updated < 0L) return Transaction.abort()
                    currentData.child("coins").value = updated
                    if (countAsEarned && deltaCoins > 0) {
                        val earned = currentData.child("totalCoinsEarned").getValue(Long::class.java) ?: 0L
                        currentData.child("totalCoinsEarned").value = earned + deltaCoins
                    }
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (error != null) cont.resume(-1L)
                    else if (!committed) cont.resume(-2L)
                    else cont.resume(snapshot?.child("coins")?.getValue(Long::class.java) ?: 0L)
                }
            })
        }
        if (newBalance == -2L) error("Insufficient coin balance.")
        if (newBalance < 0L) error("Transaction failed. Please try again.")

        if (countAsEarned && deltaCoins > 0) {
            val totalEarned = db.getReference("users/$uid/totalCoinsEarned").get().await().getValue(Long::class.java) ?: 0L
            db.getReference("leaderboard/$uid/totalCoinsEarned").setValue(totalEarned).await()
        }
        logTransaction(uid, txType, txTitle, deltaCoins, 0.0)
        newBalance
    }

    suspend fun addGems(uid: String, field: String, delta: Int): Result<Int> = runCatching {
        val ref = db.getReference("users/$uid/$field")
        val current = (ref.get().await().getValue(Long::class.java) ?: 0L).toInt()
        val updated = (current + delta).coerceAtLeast(0)
        ref.setValue(updated).await()
        updated
    }

    suspend fun claimGemGoal(uid: String, gemField: String, goal: Int, rewardCoins: Long, txType: String, txTitle: String): Result<Unit> = runCatching {
        val currentGems = (db.getReference("users/$uid/$gemField").get().await().getValue(Long::class.java) ?: 0L).toInt()
        if (currentGems < goal) error("Collect $goal gems first to claim this reward.")
        db.getReference("users/$uid/$gemField").setValue((currentGems - goal).coerceAtLeast(0)).await()
        adjustCoins(uid, rewardCoins, countAsEarned = true, txType = txType, txTitle = txTitle).getOrThrow()
    }

    // --- Tasks ---

    suspend fun startOrSubmitTask(uid: String, task: AppTask, proofNote: String, isResubmit: Boolean): Result<UserTaskRecord> = runCatching {
        val now = System.currentTimeMillis()
        val existingSnap = db.getReference("userTasks/$uid/${task.id}").get().await()
        val prevAttempts = (existingSnap.child("attemptsUsed").getValue(Long::class.java) ?: 0L).toInt()
        if (isResubmit && prevAttempts >= 3) error("Maximum 3 attempts reached for this task.")

        val startedAt = existingSnap.child("startedAt").getValue(Long::class.java) ?: now
        val expiresAt = if (existingSnap.exists()) {
            existingSnap.child("expiresAt").getValue(Long::class.java) ?: (now + TimeUnit.DAYS.toMillis(7))
        } else {
            now + TimeUnit.DAYS.toMillis(7)
        }
        val nextAttempts = if (proofNote.isNotBlank()) prevAttempts + 1 else prevAttempts
        val record = UserTaskRecord(
            taskId = task.id,
            appName = task.appName,
            title = task.title,
            prizeAmount = task.prizeAmount,
            currency = task.currency,
            status = "PENDING",
            attemptsUsed = nextAttempts,
            maxAttempts = 3,
            proofNote = proofNote,
            rejectionReason = "",
            startedAt = startedAt,
            expiresAt = expiresAt,
            updatedAt = now
        )
        db.getReference("userTasks/$uid/${task.id}").setValue(record).await()
        record
    }

    suspend fun removeExpiredUserTask(uid: String, taskId: String) {
        db.getReference("userTasks/$uid/$taskId").removeValue().await()
    }

    // --- Wallet ---

    suspend fun savePayoutMethods(uid: String, methods: PayoutMethods): Result<Unit> = runCatching {
        db.getReference("users/$uid/payoutMethods").setValue(methods).await()
    }

    suspend fun convertCoinsToInr(uid: String, coinsToConvert: Long, config: AppConfig): Result<Double> = runCatching {
        if (coinsToConvert < config.minConvertCoins) {
            error("Minimum ${config.minConvertCoins} coins required to convert.")
        }
        val inrGain = coinsToConvert.toDouble() / config.coinsPerInr.coerceAtLeast(1L).toDouble()
        adjustCoins(uid, -coinsToConvert, countAsEarned = false, txType = "CONVERT", txTitle = "Converted $coinsToConvert coins to INR").getOrThrow()
        val inrRef = db.getReference("users/$uid/inrBalance")
        val curInr = inrRef.get().await().getValue(Double::class.java) ?: 0.0
        inrRef.setValue(curInr + inrGain).await()
        inrGain
    }

    suspend fun requestWithdrawal(
        uid: String,
        userName: String,
        inrAmount: Double,
        method: String,
        payoutMethods: PayoutMethods,
        config: AppConfig
    ): Result<Unit> = runCatching {
        val cpi = config.coinsPerInr.coerceAtLeast(1L).toDouble()
        val minInr = config.minWithdrawalCoins.toDouble() / cpi
        val maxInr = config.maxWithdrawalCoins.toDouble() / cpi

        if (inrAmount < minInr) error("Minimum withdrawal is ₹${String.format("%.2f", minInr)} INR.")
        if (inrAmount > maxInr) error("Maximum withdrawal is ₹${String.format("%.2f", maxInr)} INR.")
        if (method == "UPI" && payoutMethods.upiId.isBlank()) error("Please save your UPI ID in Wallet Settings first.")
        if (method == "BANK" && (payoutMethods.bankAccountNumber.isBlank() || payoutMethods.bankIfsc.isBlank())) {
            error("Please save your Bank Account details in Wallet Settings first.")
        }

        // Atomically deduct from converted INR balance (inrBalance)
        val remainingInr = suspendCancellableCoroutine<Double> { cont ->
            val ref = db.getReference("users/$uid/inrBalance")
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Double::class.java)
                        ?: currentData.getValue(Long::class.java)?.toDouble() ?: 0.0
                    if (current + 0.0001 < inrAmount) return Transaction.abort()
                    val updated = ((current - inrAmount) * 100.0).toLong() / 100.0
                    currentData.value = updated.coerceAtLeast(0.0)
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (error != null) cont.resume(-1.0)
                    else if (!committed) cont.resume(-2.0)
                    else cont.resume(snapshot?.getValue(Double::class.java) ?: 0.0)
                }
            })
        }
        if (remainingInr == -2.0) error("Insufficient INR balance. Convert your coins to INR first.")
        if (remainingInr < 0.0) error("Withdrawal failed. Please try again.")

        val equivalentCoins = (inrAmount * cpi).toLong()
        logTransaction(
            uid = uid,
            type = "WITHDRAWAL",
            title = "Withdrawal ₹${String.format("%.2f", inrAmount)} via $method",
            amountCoins = 0L,
            amountInr = -inrAmount,
            status = "PENDING"
        )

        val wId = db.getReference("withdrawals").push().key ?: UUID.randomUUID().toString()
        val payload = mapOf(
            "id" to wId,
            "uid" to uid,
            "userName" to userName,
            "method" to method,
            "coins" to equivalentCoins,
            "inrAmount" to inrAmount,
            "upiId" to payoutMethods.upiId,
            "bankDetails" to mapOf(
                "accountHolder" to payoutMethods.bankAccountHolder,
                "bankName" to payoutMethods.bankName,
                "accountNumber" to payoutMethods.bankAccountNumber,
                "ifsc" to payoutMethods.bankIfsc,
                "phone" to payoutMethods.bankPhone
            ),
            "status" to "PENDING",
            "createdAt" to System.currentTimeMillis()
        )
        db.getReference("withdrawals/$wId").setValue(payload).await()
    }

    // --- Profile & Account ---

    suspend fun updateProfile(uid: String, fullName: String, gender: String, age: Int): Result<Unit> = runCatching {
        val updates = mapOf(
            "fullName" to fullName.trim(),
            "gender" to gender,
            "age" to age
        )
        db.getReference("users/$uid").updateChildren(updates).await()
        db.getReference("leaderboard/$uid/fullName").setValue(fullName.trim()).await()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        val email = user.email ?: error("No email linked to this account.")
        val cred = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(cred).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun requestAccountDeletion(password: String, reason: String): Result<Long> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        val email = user.email
        if (!email.isNullOrBlank() && password.isNotBlank()) {
            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        }
        val now = System.currentTimeMillis()
        val deleteAt = now + TimeUnit.DAYS.toMillis(7)
        val req = DeleteRequest(
            requested = true,
            requestedAt = now,
            scheduledDeleteAt = deleteAt,
            reason = reason.trim()
        )
        db.getReference("users/${user.uid}/deleteRequest").setValue(req).await()
        deleteAt
    }

    suspend fun cancelAccountDeletion(uid: String): Result<Unit> = runCatching {
        db.getReference("users/$uid/deleteRequest").setValue(DeleteRequest()).await()
    }

    suspend fun markNotificationRead(uid: String, notifId: String) {
        db.getReference("notifications/$uid/$notifId/read").setValue(true).await()
    }

    suspend fun logTransaction(uid: String, type: String, title: String, amountCoins: Long, amountInr: Double, status: String = "COMPLETED") {
        val key = db.getReference("transactions/$uid").push().key ?: UUID.randomUUID().toString()
        db.getReference("transactions/$uid/$key").setValue(
            mapOf(
                "type" to type,
                "title" to title,
                "amountCoins" to amountCoins,
                "amountInr" to amountInr,
                "status" to status,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }


    suspend fun applyReferralCodeAfterAuth(
        uid: String,
        fullName: String,
        email: String,
        referralCodeInput: String,
        referralConfig: ReferralConfig,
        coinsPerInr: Long
    ): Result<Long> = runCatching {
        if (!referralConfig.enabled) error("Referral program is currently paused.")
        val cleanCode = referralCodeInput.trim().uppercase(Locale.US)
        if (cleanCode.isBlank()) error("Please enter a referral code.")

        val refSnap = db.getReference("referralCodes/$cleanCode").get().await()
        val referrerUid = refSnap.getValue(String::class.java) ?: ""
        if (referrerUid.isBlank()) error("Invalid referral code.")
        if (referrerUid == uid) error("You cannot use your own referral code.")

        val userSnap = db.getReference("users/$uid").get().await()
        val alreadyReferred = userSnap.child("referredByUid").getValue(String::class.java).orEmpty()
        if (alreadyReferred.isNotBlank()) error("Referral code already applied on this account.")

        val now = System.currentTimeMillis()
        db.getReference("users/$uid").updateChildren(
            mapOf(
                "referredByUid" to referrerUid,
                "referredByCode" to cleanCode,
                "referralPromptDone" to true
            )
        ).await()

        val rewardInr = if (coinsPerInr > 0) referralConfig.referrerRewardCoins.toDouble() / coinsPerInr else 0.0
        db.getReference("referrals/$referrerUid/$uid").setValue(
            mapOf(
                "referredName" to fullName.ifBlank { "PlayReward User" },
                "referredEmail" to email,
                "status" to "PENDING",
                "rewardCoins" to referralConfig.referrerRewardCoins,
                "rewardInr" to rewardInr,
                "createdAt" to now,
                "completedAt" to 0L
            )
        ).await()

        val bonus = referralConfig.signupBonusCoins
        if (bonus > 0) {
            adjustCoins(uid, bonus, countAsEarned = true, txType = "SIGNUP_BONUS", txTitle = "Referral Signup Bonus").getOrThrow()
        }
        bonus
    }

    suspend fun skipReferralPrompt(uid: String): Result<Unit> = runCatching {
        db.getReference("users/$uid/referralPromptDone").setValue(true).await()
    }

    suspend fun savePersonalDetails(uid: String, gender: String, age: Int): Result<Unit> = runCatching {
        require(gender.isNotBlank()) { "Please select your gender." }
        require(age in 13..100) { "Please enter a valid age (13-100)." }
        db.getReference("users/$uid").updateChildren(
            mapOf("gender" to gender, "age" to age)
        ).await()
    }


    // --- Real-Time 1v1 PvP Tic-Tac-Toe Matchmaking (Zero Bots, 100% Spark Plan RTDB) ---

    suspend fun findOrJoinPvpQueue(
        uid: String,
        fullName: String,
        tier: TicTacToeTier
    ): Result<String?> = runCatching {
        // Clear any stale previous match notification for this user
        db.getReference("tttQueueResult/$uid").removeValue().await()

        val now = System.currentTimeMillis()
        val queueRef = db.getReference("tttQueue/${tier.id}")
        var matchedRoomId: String? = null
        var matchedOpponentUid: String? = null
        var matchedOpponentName: String? = null

        val txResult = suspendCancellableCoroutine<Boolean> { cont ->
            queueRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val waitingUid = currentData.child("uid").getValue(String::class.java)
                    val waitingName = currentData.child("name").getValue(String::class.java) ?: "Player"
                    val waitingAt = currentData.child("joinedAt").getValue(Long::class.java) ?: 0L
                    val isFresh = waitingUid != null && waitingUid != uid && (now - waitingAt) < 90_000L

                    if (isFresh && waitingUid != null) {
                        val roomId = "room_" + UUID.randomUUID().toString().replace("-", "").take(12)
                        matchedRoomId = roomId
                        matchedOpponentUid = waitingUid
                        matchedOpponentName = waitingName
                        currentData.value = null // Atomically claim opponent from queue
                    } else {
                        matchedRoomId = null
                        currentData.value = mapOf(
                            "uid" to uid,
                            "name" to fullName.ifBlank { "Player" },
                            "joinedAt" to now
                        )
                    }
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    cont.resume(error == null && committed)
                }
            })
        }

        if (!txResult) error("Matchmaking busy. Please retry.")

        val roomId = matchedRoomId
        val oppUid = matchedOpponentUid
        if (roomId != null && oppUid != null) {
            val roomPayload = mapOf(
                "roomId" to roomId,
                "tierId" to tier.id,
                "tierLabel" to tier.label,
                "difficulty" to tier.difficulty,
                "entryCoins" to tier.entryCoins,
                "winCoins" to tier.winCoins,
                "playerXUid" to oppUid,
                "playerXName" to (matchedOpponentName ?: "Player"),
                "playerOUid" to uid,
                "playerOName" to fullName.ifBlank { "Player" },
                "board" to List(9) { "" },
                "turn" to "X",
                "status" to "PLAYING",
                "winningLine" to emptyList<Int>(),
                "lastMoveIndex" to -1,
                "lastMoveAt" to now,
                "createdAt" to now
            )
            db.getReference("tttRooms/$roomId").setValue(roomPayload).await()
            db.getReference("tttQueueResult/$oppUid").setValue(roomId).await()
            roomId
        } else {
            null // Waiting in queue for a real player to join
        }
    }

    suspend fun leavePvpQueue(uid: String, tierId: String) {
        try {
            val ref = db.getReference("tttQueue/$tierId")
            val snap = ref.get().await()
            if (snap.child("uid").getValue(String::class.java) == uid) {
                ref.removeValue().await()
            }
            db.getReference("tttQueueResult/$uid").removeValue().await()
        } catch (_: Exception) {
        }
    }

    fun observeQueueMatchRoom(uid: String): Flow<String?> = observeNode("tttQueueResult/$uid") { snap ->
        snap.getValue(String::class.java)?.takeIf { it.isNotBlank() }
    }

    fun observePvpRoom(roomId: String): Flow<PvpRoom?> = observeNode("tttRooms/$roomId") { snap ->
        if (!snap.exists()) return@observeNode null
        val boardList = (0..8).map { i -> snap.child("board").child(i.toString()).getValue(String::class.java) ?: "" }
        val lineList = snap.child("winningLine").children.mapNotNull { it.getValue(Long::class.java)?.toInt() }
        PvpRoom(
            roomId = roomId,
            tierId = snap.child("tierId").getValue(String::class.java) ?: "",
            tierLabel = snap.child("tierLabel").getValue(String::class.java) ?: "Arena",
            difficulty = snap.child("difficulty").getValue(String::class.java) ?: "",
            entryCoins = snap.child("entryCoins").getValue(Long::class.java) ?: 0L,
            winCoins = snap.child("winCoins").getValue(Long::class.java) ?: 0L,
            playerXUid = snap.child("playerXUid").getValue(String::class.java) ?: "",
            playerXName = snap.child("playerXName").getValue(String::class.java) ?: "Player X",
            playerOUid = snap.child("playerOUid").getValue(String::class.java) ?: "",
            playerOName = snap.child("playerOName").getValue(String::class.java) ?: "Player O",
            board = boardList,
            turn = snap.child("turn").getValue(String::class.java) ?: "X",
            status = snap.child("status").getValue(String::class.java) ?: "PLAYING",
            winningLine = lineList,
            lastMoveIndex = (snap.child("lastMoveIndex").getValue(Long::class.java) ?: -1L).toInt(),
            lastMoveAt = snap.child("lastMoveAt").getValue(Long::class.java) ?: 0L,
            createdAt = snap.child("createdAt").getValue(Long::class.java) ?: 0L
        )
    }

    suspend fun makePvpMove(roomId: String, cellIndex: Int, mySymbol: String): Result<Unit> = runCatching {
        val winLines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        val ok = suspendCancellableCoroutine<Boolean> { cont ->
            db.getReference("tttRooms/$roomId").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val status = currentData.child("status").getValue(String::class.java) ?: "PLAYING"
                    val turn = currentData.child("turn").getValue(String::class.java) ?: "X"
                    if (status != "PLAYING" || turn != mySymbol) return Transaction.abort()

                    val board = (0..8).map { i -> currentData.child("board").child(i.toString()).getValue(String::class.java) ?: "" }.toMutableList()
                    if (cellIndex !in 0..8 || board[cellIndex].isNotEmpty()) return Transaction.abort()

                    board[cellIndex] = mySymbol
                    currentData.child("board").value = board
                    currentData.child("lastMoveIndex").value = cellIndex
                    currentData.child("lastMoveAt").value = System.currentTimeMillis()

                    var winLine: List<Int>? = null
                    for (line in winLines) {
                        if (board[line[0]] == mySymbol && board[line[1]] == mySymbol && board[line[2]] == mySymbol) {
                            winLine = line
                            break
                        }
                    }
                    when {
                        winLine != null -> {
                            currentData.child("status").value = if (mySymbol == "X") "X_WIN" else "O_WIN"
                            currentData.child("winningLine").value = winLine
                        }
                        board.none { it.isEmpty() } -> {
                            currentData.child("status").value = "DRAW"
                        }
                        else -> {
                            currentData.child("turn").value = if (mySymbol == "X") "O" else "X"
                        }
                    }
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    cont.resume(error == null && committed)
                }
            })
        }
        if (!ok) error("Move not allowed.")
    }

    suspend fun forfeitPvpRoom(roomId: String, mySymbol: String) {
        try {
            val statusRef = db.getReference("tttRooms/$roomId/status")
            val cur = statusRef.get().await().getValue(String::class.java)
            if (cur == "PLAYING") {
                statusRef.setValue(if (mySymbol == "X") "FORFEIT_X" else "FORFEIT_O").await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun claimPvpTimeoutWin(roomId: String, opponentSymbol: String) {
        try {
            val statusRef = db.getReference("tttRooms/$roomId/status")
            val cur = statusRef.get().await().getValue(String::class.java)
            if (cur == "PLAYING") {
                statusRef.setValue(if (opponentSymbol == "X") "FORFEIT_X" else "FORFEIT_O").await()
            }
        } catch (_: Exception) {
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
