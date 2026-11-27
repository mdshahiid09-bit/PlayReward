package com.playreward.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playreward.app.audio.SoundEffectsManager
import com.playreward.app.data.model.*
import com.playreward.app.service.PlayRewardForegroundService
import com.playreward.app.data.prefs.UserPreferences
import com.playreward.app.data.repository.FirebaseRepository
import com.playreward.app.notifications.LocalNotificationHelper
import com.playreward.app.ui.components.ToastType
import com.playreward.app.ui.theme.ThemeMode
import com.playreward.app.update.DownloadProgressState
import com.playreward.app.update.GithubUpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class ToastState(val message: String, val type: ToastType = ToastType.INFO)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repo = FirebaseRepository()
    val prefs = UserPreferences(application)
    val updateManager = GithubUpdateManager(application)
    val soundFx = SoundEffectsManager(application)

    private val _splashDone = MutableStateFlow(false)
    val splashDone: StateFlow<Boolean> = _splashDone.asStateFlow()

    val soundEnabled: StateFlow<Boolean> = prefs.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val bgServiceEnabled: StateFlow<Boolean> = prefs.bgServiceEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _toast = MutableStateFlow<ToastState?>(null)
    val toast: StateFlow<ToastState?> = _toast.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.LIGHT)

    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val localAdminBypassUid: StateFlow<String> = prefs.localAdminBypassUidFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val appConfig: StateFlow<AppConfig> = repo.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val referralConfig: StateFlow<ReferralConfig> = repo.observeReferralConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReferralConfig())

    val startupPopup: StateFlow<StartupPopupConfig> = repo.observeStartupPopup()
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartupPopupConfig())

    val onboardingPages: StateFlow<List<OnboardingPage>> = repo.observeOnboardingPages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banners: StateFlow<List<BannerItem>> = repo.observeBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val legalConfig: StateFlow<LegalConfig> = repo.observeLegalConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LegalConfig())

    val supportChannels: StateFlow<List<SupportChannel>> = repo.observeSupportChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizSettings: StateFlow<QuizSettings> = repo.observeQuizSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuizSettings())

    val quizCategories: StateFlow<List<QuizCategory>> = repo.observeQuizCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val megaOfferConfig: StateFlow<MegaOfferConfig> = repo.observeMegaOfferConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MegaOfferConfig())

    val superOfferConfig: StateFlow<SuperOfferConfig> = repo.observeSuperOfferConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuperOfferConfig())

    val taskCategories: StateFlow<List<TaskCategory>> = repo.observeTaskCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<AppTask>> = repo.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repo.observeLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUid = MutableStateFlow(repo.currentUser?.uid)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = _currentUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else repo.observeUserProfile(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userTasks: StateFlow<List<UserTaskRecord>> = _currentUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeUserTasks(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referrals: StateFlow<List<ReferralRecord>> = _currentUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeReferrals(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionItem>> = _currentUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeTransactions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotificationItem>> = _currentUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeNotifications(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableUpdate = MutableStateFlow<GithubReleaseInfo?>(null)
    val availableUpdate: StateFlow<GithubReleaseInfo?> = _availableUpdate.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadState: StateFlow<DownloadProgressState> = _downloadState.asStateFlow()

    private val _popupShownThisSession = MutableStateFlow(false)
    val popupShownThisSession: StateFlow<Boolean> = _popupShownThisSession.asStateFlow()

    private val seenTaskStatuses = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            soundEnabled.collect { soundFx.soundEnabled = it }
        }
        viewModelScope.launch {
            hapticsEnabled.collect { soundFx.hapticsEnabled = it }
        }
        viewModelScope.launch {
            bgServiceEnabled.collect { enabled ->
                if (enabled) PlayRewardForegroundService.start(getApplication())
                else PlayRewardForegroundService.stop(getApplication())
            }
        }
        viewModelScope.launch {
            if (!prefs.isKeepLoggedIn() && repo.currentUser != null) {
                repo.signOut()
                _currentUid.value = null
            }
        }
        viewModelScope.launch {
            appConfig
                .map { Triple(it.forceUpdateEnabled, it.githubOwner, it.githubRepo) }
                .distinctUntilChanged()
                .collect { (enabled, owner, repo) ->
                    if (enabled) {
                        val res = updateManager.checkLatestReleaseDetailed(owner, repo)
                        if (res.release != null) {
                            _availableUpdate.value = res.release
                        }
                    } else {
                        _availableUpdate.value = null
                    }
                }
        }
        viewModelScope.launch {
            userTasks.collect { list ->
                val uid = _currentUid.value ?: return@collect
                val now = System.currentTimeMillis()
                val ctx = getApplication<Application>()
                list.forEach { rec ->
                    val prev = seenTaskStatuses[rec.taskId]
                    if (prev != null && prev != rec.status) {
                        when (rec.status) {
                            "COMPLETED" -> LocalNotificationHelper.showNotification(
                                ctx, rec.taskId.hashCode(),
                                "Task Approved!",
                                "${rec.title} was approved and your reward has been credited."
                            )
                            "REJECTED" -> LocalNotificationHelper.showNotification(
                                ctx, rec.taskId.hashCode(),
                                "Task Rejected",
                                "${rec.title} was rejected. ${rec.rejectionReason.ifBlank { "Check Task History to resubmit." }}"
                            )
                        }
                    }
                    seenTaskStatuses[rec.taskId] = rec.status

                    // Auto-expire PENDING after 7 days back to available, or REJECTED after 7 days
                    val expiredPending = rec.status == "PENDING" && rec.expiresAt > 0 && now > rec.expiresAt
                    val expiredRejected = rec.status == "REJECTED" && rec.updatedAt > 0 &&
                        (now - rec.updatedAt) > TimeUnit.DAYS.toMillis(7)
                    if (expiredPending || expiredRejected) {
                        repo.removeExpiredUserTask(uid, rec.taskId)
                    }
                }
            }
        }
    }

    fun markSplashDone() { _splashDone.value = true }
    fun setSoundEnabled(v: Boolean) { viewModelScope.launch { prefs.setSoundEnabled(v) } }
    fun setHapticsEnabled(v: Boolean) { viewModelScope.launch { prefs.setHapticsEnabled(v) } }
    fun setBgServiceEnabled(v: Boolean) { viewModelScope.launch { prefs.setBgServiceEnabled(v) } }

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        when (type) {
            ToastType.SUCCESS -> soundFx.playSuccess()
            ToastType.ERROR -> soundFx.playError()
            ToastType.INFO -> soundFx.playClick()
        }
        viewModelScope.launch {
            _toast.value = ToastState(message, type)
            delay(3000)
            if (_toast.value?.message == message) _toast.value = null
        }
    }

    fun syncAuthUid() {
        _currentUid.value = repo.currentUser?.uid
    }

    fun markPopupShown() {
        _popupShownThisSession.value = true
    }

    fun refreshUpdateCheckSilently() {
        viewModelScope.launch {
            val cfg = appConfig.value
            if (!cfg.forceUpdateEnabled) return@launch
            val res = updateManager.checkLatestReleaseDetailed(cfg.githubOwner, cfg.githubRepo)
            if (res.release != null) _availableUpdate.value = res.release
        }
    }

    fun checkUpdateManually() {
        viewModelScope.launch {
            val cfg = appConfig.value
            showToast("Checking GitHub releases...", ToastType.INFO)
            val res = updateManager.checkLatestReleaseDetailed(cfg.githubOwner, cfg.githubRepo)
            when {
                res.release != null -> _availableUpdate.value = res.release
                !res.errorMessage.isNullOrBlank() -> showToast(res.errorMessage, ToastType.ERROR)
                else -> showToast("You are already on the latest version!", ToastType.SUCCESS)
            }
        }
    }

    fun startUpdateDownload(release: GithubReleaseInfo) {
        viewModelScope.launch {
            updateManager.downloadUpdate(release).collect { state ->
                _downloadState.value = state
                if (state is DownloadProgressState.Completed) {
                    updateManager.installApk(state.file)
                }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone(true) }
    }

    fun saveAdminMaintenanceBypass(uid: String) {
        viewModelScope.launch {
            val res = repo.saveAdminBypassUid(uid)
            if (res.isSuccess) {
                prefs.setLocalAdminBypassUid(uid)
                showToast("Admin UID saved. Maintenance bypass active.", ToastType.SUCCESS)
            } else {
                showToast(res.exceptionOrNull()?.message ?: "Failed to save UID.", ToastType.ERROR)
            }
        }
    }

    fun logout() {
        repo.signOut()
        _currentUid.value = null
        showToast("Signed out successfully.", ToastType.INFO)
    }
}
