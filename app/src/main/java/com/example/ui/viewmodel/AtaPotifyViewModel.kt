package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.error.unwrapFirestoreException
import com.example.data.model.AccessRequest
import com.example.data.model.AllTimeWrappedSummary
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.FamilyGroup
import com.example.data.model.ListeningStat
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.PlanConfig
import com.example.data.model.ReceiptSlip
import com.example.data.model.UserProfile
import com.example.data.model.WhitelistMember
import com.example.data.model.buildVerifiedPaymentReceiptText
import com.example.data.model.buildWhatsAppMonthlySummaryText
import com.example.data.model.calculateAllTimeWrappedSummary
import com.example.data.model.calculateLongestStreakStanding
import com.example.data.model.calculatePaymentStreaks
import com.example.data.model.resolveEffectiveFamilyGroups
import com.example.data.repository.AccessResolution
import com.example.data.repository.AuthRepository
import com.example.data.repository.PaymentRepository
import com.example.data.service.AppUpdateCheckResult
import com.example.data.service.AppUpdateService
import com.example.data.service.GitHubAppRelease
import com.example.data.service.ImageCompressor
import com.example.data.service.SpotifyStatsService
import com.example.data.worker.BillingReminderNotifier
import com.example.data.worker.BillingReminderScheduler
import com.example.data.worker.SpotifySyncScheduler
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthGateUiState {
    data object CheckingAccess : AuthGateUiState
    data class AwaitingApproval(
        val info: AccessResolution.AwaitingApproval,
        val isSavingDetails: Boolean = false,
        val infoMessage: String? = null,
    ) : AuthGateUiState
    data class Denied(val email: String, val errorMessage: String? = null) : AuthGateUiState
    data class Authorized(val session: AccessResolution.Authorized) : AuthGateUiState
}

data class OverdueReminderCandidate(
    val member: MemberPaymentStatus,
    val phoneDigits: String,
    val pendingDays: Long,
    val prefilledMessage: String,
)

data class ReceiptDialogState(
    val monthId: String,
    val member: MemberPaymentStatus,
    val isLoading: Boolean = true,
    val slip: ReceiptSlip? = null,
    val error: String? = null,
)

class AuthGateViewModel(
    private val authRepository: AuthRepository,
    private val firebaseUser: FirebaseUser,
) : ViewModel() {

    private val _authGateState = MutableStateFlow<AuthGateUiState>(AuthGateUiState.CheckingAccess)
    val authGateState: StateFlow<AuthGateUiState> = _authGateState.asStateFlow()

    private var approvalWatchJob: Job? = null

    init {
        verifyAccess()
    }

    fun verifyAccess() {
        approvalWatchJob?.cancel()
        approvalWatchJob = null
        viewModelScope.launch {
            _authGateState.value = AuthGateUiState.CheckingAccess
            val result = authRepository.resolveAccessAndSyncProfile(firebaseUser)
            _authGateState.value = result.fold(
                onSuccess = { resolution ->
                    when (resolution) {
                        is AccessResolution.Authorized -> AuthGateUiState.Authorized(resolution)
                        is AccessResolution.AwaitingApproval -> {
                            startWatchingForApproval(resolution.email)
                            AuthGateUiState.AwaitingApproval(info = resolution)
                        }
                        is AccessResolution.Denied -> AuthGateUiState.Denied(resolution.email)
                    }
                },
                onFailure = { error ->
                    Log.w("AuthGateVM", "Access verification error", error)
                    AuthGateUiState.Denied(
                        email = firebaseUser.email.orEmpty(),
                        errorMessage = error.localizedMessage ?: "Unable to verify access",
                    )
                },
            )
        }
    }

    private fun startWatchingForApproval(emailLower: String) {
        approvalWatchJob?.cancel()
        approvalWatchJob = viewModelScope.launch {
            authRepository.observeOwnWhitelistApproval(emailLower).collect { isApproved ->
                if (isApproved) {
                    verifyAccess()
                }
            }
        }
    }

    fun updatePendingDetails(displayName: String, phone: String, spotifyEmail: String) {
        val current = _authGateState.value as? AuthGateUiState.AwaitingApproval ?: return
        viewModelScope.launch {
            _authGateState.value = current.copy(isSavingDetails = true, infoMessage = null)
            val res = authRepository.updatePendingAccessRequestDetails(
                displayName = displayName,
                phone = phone,
                spotifyEmail = spotifyEmail,
            )
            _authGateState.value = res.fold(
                onSuccess = {
                    val latest = _authGateState.value as? AuthGateUiState.AwaitingApproval
                    latest?.copy(
                        info = latest.info.copy(
                            displayName = displayName.trim(),
                            phone = AtaPotifyConstants.sanitizeWhatsAppDigits(phone),
                            spotifyEmail = AtaPotifyConstants.normalizeEmail(spotifyEmail),
                            requestStatus = AtaPotifyConstants.STATUS_PENDING,
                        ),
                        isSavingDetails = false,
                        infoMessage = "Details updated & sent to Admin",
                    ) ?: current
                },
                onFailure = { error ->
                    current.copy(
                        isSavingDetails = false,
                        infoMessage = error.localizedMessage ?: "Unable to save details",
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AtaPotifyViewModel(
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository,
    private val spotifyService: SpotifyStatsService,
    private val session: AccessResolution.Authorized,
) : ViewModel() {

    private val _selectedHistoryMonthId = MutableStateFlow<String?>(null)
    private val _selectedGroupFilter = MutableStateFlow(AtaPotifyConstants.GROUP_ALL)
    val selectedGroupFilter: StateFlow<String> = _selectedGroupFilter.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _receiptDialog = MutableStateFlow<ReceiptDialogState?>(null)
    val receiptDialog: StateFlow<ReceiptDialogState?> = _receiptDialog.asStateFlow()

    private val _showAutoReminderSheet = MutableStateFlow(false)
    val showAutoReminderSheet: StateFlow<Boolean> = _showAutoReminderSheet.asStateFlow()

    private val _availableUpdate = MutableStateFlow<GitHubAppRelease?>(null)
    val availableUpdate: StateFlow<GitHubAppRelease?> = _availableUpdate.asStateFlow()

    private val _spotifyEmailPromptDismissed = MutableStateFlow(false)
    private val _allTimeWrapped = MutableStateFlow<AllTimeWrappedSummary?>(null)
    val allTimeWrapped: StateFlow<AllTimeWrappedSummary?> = _allTimeWrapped.asStateFlow()

    private val _paymentStreaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    val paymentStreaks: StateFlow<Map<String, Int>> = _paymentStreaks.asStateFlow()

    private var hasAutoPromptedOverdueThisSession = false
    private var hasCheckedUpdateThisSession = false
    private val dismissedUpdateTags = mutableSetOf<String>()

    private val _spotifyConnected = MutableStateFlow(spotifyService.isConnected)
    val spotifyConnected: StateFlow<Boolean> = _spotifyConnected.asStateFlow()

    private val _spotifyName = MutableStateFlow(spotifyService.connectedSpotifyName)
    val spotifyName: StateFlow<String> = _spotifyName.asStateFlow()

    val isSpotifyClientConfigured: Boolean
        get() = spotifyService.isClientIdConfigured

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val userProfile: StateFlow<UserProfile?> = authRepository.observeUserProfile(session.uid)
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", fsErr ?: error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val needsSpotifyEmailPrompt: StateFlow<Boolean> = combine(
        userProfile,
        _spotifyEmailPromptDismissed,
    ) { profile, dismissed ->
        if (dismissed || profile == null) false
        else profile.spotifyEmail.isBlank() && session.spotifyEmail.isBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val planConfig: StateFlow<PlanConfig> = paymentRepository.observePlanConfig()
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Plan config unavailable yet: ${(fsErr ?: error).message}")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlanConfig())

    private val rawFamilyGroups: StateFlow<List<FamilyGroup>> = paymentRepository.observeFamilyGroups()
        .catch { error ->
            val fsErr = error.unwrapFirestoreException()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Family groups unavailable yet: ${(fsErr ?: error).message}")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val whitelist: StateFlow<List<WhitelistMember>> =
        paymentRepository.observeWhitelist(userEmailLower = session.email, isAdmin = session.isAdmin)
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing whitelist", fsErr ?: error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val pendingAccessRequests: StateFlow<List<AccessRequest>> = if (session.isAdmin) {
        paymentRepository.observePendingAccessRequests()
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing pending access requests", fsErr ?: error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val paymentMonths: StateFlow<List<PaymentMonth>> =
        paymentRepository.observePaymentMonths(session.email, session.isAdmin)
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing months", fsErr ?: error)
                _snackbarEvents.tryEmit((fsErr ?: error).localizedMessage ?: "Failed to load months")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val currentMonth: StateFlow<PaymentMonth?> = paymentMonths
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val selectedHistoryMonth: StateFlow<PaymentMonth?> = combine(
        paymentMonths,
        _selectedHistoryMonthId,
    ) { months, selectedId ->
        if (months.isEmpty()) null
        else months.firstOrNull { it.monthId == selectedId } ?: months.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val currentMonthMembers: StateFlow<List<MemberPaymentStatus>> = currentMonth
        .flatMapLatest { month ->
            if (month == null) flowOf(emptyList())
            else paymentRepository.observeMonthMembers(month.monthId, session.email, session.isAdmin)
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing current month members", fsErr ?: error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val familyGroups: StateFlow<List<FamilyGroup>> = combine(
        rawFamilyGroups,
        planConfig,
        whitelist,
        currentMonthMembers,
    ) { raw, cfg, wl, members ->
        resolveEffectiveFamilyGroups(
            rawGroups = raw,
            defaultAmountLkr = cfg.monthlyAmountLkr,
            whitelist = wl,
            members = members,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        resolveEffectiveFamilyGroups(emptyList()),
    )

    val currentMonthStats: StateFlow<List<ListeningStat>> = currentMonth
        .flatMapLatest { month ->
            if (month == null) flowOf(emptyList())
            else paymentRepository.observeMonthStats(month.monthId, session.email, session.isAdmin)
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing current month stats", fsErr ?: error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val historyMonthMembers: StateFlow<List<MemberPaymentStatus>> = selectedHistoryMonth
        .flatMapLatest { month ->
            if (month == null) flowOf(emptyList())
            else paymentRepository.observeMonthMembers(month.monthId, session.email, session.isAdmin)
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing history month members", fsErr ?: error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val historyMonthStats: StateFlow<List<ListeningStat>> = selectedHistoryMonth
        .flatMapLatest { month ->
            if (month == null) flowOf(emptyList())
            else paymentRepository.observeMonthStats(month.monthId, session.email, session.isAdmin)
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing history stats", fsErr ?: error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val overdueReminders: StateFlow<List<OverdueReminderCandidate>> = combine(
        currentMonth,
        currentMonthMembers,
        whitelist,
    ) { month, members, roster ->
        if (!session.isAdmin || month == null) return@combine emptyList()
        val phoneByKey = roster.associate { it.memberKey to it.phone }
        val now = System.currentTimeMillis()
        members.filter { it.isOverdueForAutoReminder(now) }.map { member ->
            val phone = AtaPotifyConstants.sanitizeWhatsAppDigits(phoneByKey[member.memberKey].orEmpty())
            val days = member.pendingAgeDays(now).coerceAtLeast(AtaPotifyConstants.OVERDUE_DAYS_THRESHOLD)
            val msg = buildOverdueWhatsAppMessage(member, month.displayLabel, days)
            OverdueReminderCandidate(
                member = member,
                phoneDigits = phone,
                pendingDays = days,
                prefilledMessage = msg,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        BillingReminderScheduler.schedulePeriodicChecks(appContext)
        if (session.isAdmin) {
            viewModelScope.launch {
                paymentRepository.ensureDefaultPlanConfigIfAdmin()
                paymentRepository.ensureDefaultFamilyGroupsIfAdmin(planConfig.value.monthlyAmountLkr)
            }
            viewModelScope.launch {
                overdueReminders.collect { list ->
                    if (list.isNotEmpty() && !hasAutoPromptedOverdueThisSession) {
                        hasAutoPromptedOverdueThisSession = true
                        _showAutoReminderSheet.value = true
                    }
                }
            }
            viewModelScope.launch {
                pendingAccessRequests.collect { list ->
                    if (list.isNotEmpty()) {
                        BillingReminderNotifier.notifyAdminPendingRequestsIfNeeded(
                            context = appContext,
                            uid = session.uid,
                            pendingCount = list.size,
                        )
                    } else {
                        BillingReminderNotifier.clearAdminRequestCountCache(appContext, session.uid)
                    }
                }
            }
        } else {
            viewModelScope.launch {
                combine(currentMonth, currentMonthMembers) { month, members -> month to members }
                    .collect { (month, members) ->
                        if (month != null && members.isNotEmpty()) {
                            val selfMember = members.firstOrNull {
                                it.memberKey == session.memberKey ||
                                    it.memberEmail.equals(session.email, ignoreCase = true)
                            }
                            BillingReminderNotifier.notifyMemberForCycleIfNeeded(
                                context = appContext,
                                uid = session.uid,
                                month = month,
                                memberStatus = selfMember,
                            )
                        }
                    }
            }
        }
        if (spotifyService.isClientIdConfigured && spotifyService.isConnected) {
            SpotifySyncScheduler.schedulePeriodicSync(appContext)
        }
        viewModelScope.launch {
            currentMonth.collect { month ->
                if (month != null && spotifyService.shouldAutoSync(month.monthId)) {
                    syncSpotifyStats(silent = true)
                }
            }
        }
        viewModelScope.launch {
            combine(
                paymentMonths,
                currentMonthMembers,
                historyMonthMembers,
                currentMonthStats,
                historyMonthStats,
            ) { months, _, _, _, _ -> months }
                .collect { months ->
                    refreshAllTimeWrapped(months)
                }
        }
        checkForAppUpdate(manual = false)
    }

    fun notifyRemindersPermissionGranted() {
        BillingReminderScheduler.schedulePeriodicChecks(appContext)
        val month = currentMonth.value ?: return
        if (!session.isAdmin) {
            val selfMember = currentMonthMembers.value.firstOrNull {
                it.memberKey == session.memberKey ||
                    it.memberEmail.equals(session.email, ignoreCase = true)
            }
            BillingReminderNotifier.notifyMemberForCycleIfNeeded(
                context = appContext,
                uid = session.uid,
                month = month,
                memberStatus = selfMember,
            )
        } else if (pendingAccessRequests.value.isNotEmpty()) {
            BillingReminderNotifier.notifyAdminPendingRequestsIfNeeded(
                context = appContext,
                uid = session.uid,
                pendingCount = pendingAccessRequests.value.size,
            )
        }
    }

    private suspend fun refreshAllTimeWrapped(months: List<PaymentMonth>) {
        if (months.isEmpty()) {
            _allTimeWrapped.value = null
            _paymentStreaks.value = emptyMap()
            return
        }
        val statsByMonth = paymentRepository.fetchAllMonthsStats(
            months = months,
            userEmailLower = session.email,
            isAdmin = session.isAdmin,
        )
        val membersByMonth = paymentRepository.fetchAllMonthsMembers(
            months = months,
            userEmailLower = session.email,
            isAdmin = session.isAdmin,
        ).toMutableMap()
        currentMonth.value?.monthId?.let { activeId ->
            if (currentMonthMembers.value.isNotEmpty()) {
                membersByMonth[activeId] = currentMonthMembers.value
            }
        }
        selectedHistoryMonth.value?.monthId?.let { histId ->
            if (historyMonthMembers.value.isNotEmpty()) {
                membersByMonth[histId] = historyMonthMembers.value
            }
        }
        _paymentStreaks.value = calculatePaymentStreaks(months, membersByMonth)
        val longestStreak = calculateLongestStreakStanding(months, membersByMonth)
        val amountByMonth = months.associate { it.monthId to it.amountLkr }
        _allTimeWrapped.value = calculateAllTimeWrappedSummary(
            allStatsByMonth = statsByMonth,
            monthAmountByMonth = amountByMonth,
            longestStreak = longestStreak,
        )
    }

    fun submitInitialSpotifyEmail(spotifyEmail: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = authRepository.saveOwnSpotifyEmail(
                emailLower = session.email,
                isAdmin = session.isAdmin,
                spotifyEmail = spotifyEmail,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _spotifyEmailPromptDismissed.value = true
                    _snackbarEvents.tryEmit("Spotify email saved and reported to Admin")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save Spotify email")
                },
            )
        }
    }

    fun dismissSpotifyEmailPromptForSession() {
        _spotifyEmailPromptDismissed.value = true
    }

    fun checkForAppUpdate(manual: Boolean = false) {
        if (!manual && hasCheckedUpdateThisSession) return
        hasCheckedUpdateThisSession = true
        viewModelScope.launch {
            if (manual) _isBusy.value = true
            val result = AppUpdateService.checkForLatestRelease()
            if (manual) _isBusy.value = false
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is AppUpdateCheckResult.UpdateAvailable -> {
                            if (manual || outcome.release.tagName !in dismissedUpdateTags) {
                                _availableUpdate.value = outcome.release
                            }
                        }
                        is AppUpdateCheckResult.UpToDate -> {
                            if (manual) {
                                _snackbarEvents.tryEmit("AtaPotify v${outcome.currentVersion} is up to date")
                            }
                        }
                    }
                },
                onFailure = { error ->
                    if (manual) {
                        _snackbarEvents.tryEmit(error.localizedMessage ?: "Unable to check for updates")
                    }
                },
            )
        }
    }

    fun dismissAvailableUpdate() {
        _availableUpdate.value?.tagName?.let { dismissedUpdateTags.add(it) }
        _availableUpdate.value = null
    }

    fun downloadAppUpdate(context: Context, release: GitHubAppRelease) {
        val launched = AppUpdateService.launchUpdateDownload(context, release)
        if (launched) {
            _availableUpdate.value = null
        } else {
            _snackbarEvents.tryEmit("Unable to open browser to download ${release.tagName}")
        }
    }

    fun autoSyncSpotifyOnResume() {
        val month = currentMonth.value ?: return
        if (spotifyService.shouldAutoSync(month.monthId)) {
            syncSpotifyStats(silent = true)
        }
    }

    fun selectHistoryMonth(monthId: String) {
        _selectedHistoryMonthId.value = monthId
    }

    fun selectGroupFilter(group: String) {
        _selectedGroupFilter.value = if (group == AtaPotifyConstants.GROUP_ALL || group.isBlank()) {
            AtaPotifyConstants.GROUP_ALL
        } else {
            AtaPotifyConstants.normalizeFamilyGroup(group)
        }
    }

    fun openAutoReminderSheet() {
        _showAutoReminderSheet.value = true
    }

    fun dismissAutoReminderSheet() {
        _showAutoReminderSheet.value = false
    }

    fun updateOwnProfile(
        displayName: String,
        phone: String,
        spotifyEmail: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val monthIds = paymentMonths.value.take(3).map { it.monthId }
            val result = authRepository.updateOwnProfile(
                emailLower = session.email,
                isAdmin = session.isAdmin,
                newDisplayName = displayName,
                newPhone = phone,
                monthIdsToSync = monthIds,
                newSpotifyEmail = spotifyEmail,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    if (spotifyEmail.isNotBlank()) {
                        _spotifyEmailPromptDismissed.value = true
                    }
                    _snackbarEvents.tryEmit("Profile updated")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update profile")
                },
            )
        }
    }

    fun uploadReceiptSlip(context: Context, uri: Uri, monthId: String, member: MemberPaymentStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val compressed = ImageCompressor.compressUriToBase64(context, uri)
            compressed.fold(
                onSuccess = { img ->
                    val res = paymentRepository.uploadReceiptSlip(
                        monthId = monthId,
                        memberKey = member.memberKey,
                        memberEmail = member.memberEmail,
                        mimeType = img.mimeType,
                        imageBase64 = img.base64,
                    )
                    res.fold(
                        onSuccess = {
                            _snackbarEvents.tryEmit("Slip uploaded (${img.byteSize / 1024} KB). Status is now Verifying.")
                        },
                        onFailure = { e ->
                            _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save receipt")
                        },
                    )
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Could not process selected image")
                },
            )
            _isBusy.value = false
        }
    }

    fun openReceiptViewer(monthId: String, member: MemberPaymentStatus) {
        _receiptDialog.value = ReceiptDialogState(monthId = monthId, member = member, isLoading = true)
        viewModelScope.launch {
            val res = paymentRepository.fetchReceiptSlip(monthId, member.memberKey)
            res.fold(
                onSuccess = { slip ->
                    _receiptDialog.value = ReceiptDialogState(
                        monthId = monthId,
                        member = member,
                        isLoading = false,
                        slip = slip,
                        error = if (slip == null) "No receipt image attached yet." else null,
                    )
                },
                onFailure = { e ->
                    _receiptDialog.value = ReceiptDialogState(
                        monthId = monthId,
                        member = member,
                        isLoading = false,
                        error = e.localizedMessage ?: "Unable to load receipt",
                    )
                },
            )
        }
    }

    fun closeReceiptViewer() {
        _receiptDialog.value = null
    }

    fun confirmMemberPayment(monthId: String, memberKey: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.confirmPayment(monthId, memberKey)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _receiptDialog.value = null
                    _snackbarEvents.tryEmit("Payment confirmed as Paid")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to confirm payment")
                },
            )
        }
    }

    fun markMemberPaidInCash(monthId: String, member: MemberPaymentStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.markPaidInCash(monthId, member)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _receiptDialog.value = null
                    _snackbarEvents.tryEmit("Marked ${member.displayName} paid via Cash in Hand")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to record cash payment")
                },
            )
        }
    }

    fun recordMemberPartialPayment(
        monthId: String,
        member: MemberPaymentStatus,
        additionalAmountLkr: Long,
        isCash: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.recordPartialPayment(
                monthId = monthId,
                member = member,
                additionalAmountLkr = additionalAmountLkr,
                isCash = isCash,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { settled ->
                    val methodLabel = if (isCash) "Cash in Hand" else "Bank Transfer"
                    val msg = if (settled) {
                        "${member.displayName} settled full LKR ${member.amountLkr} ($methodLabel)"
                    } else {
                        val newTotal = (member.effectivePaidLkr + additionalAmountLkr).coerceAtMost(member.amountLkr)
                        val remaining = (member.amountLkr - newTotal).coerceAtLeast(0L)
                        "Recorded LKR $additionalAmountLkr ($methodLabel) • LKR $remaining left"
                    }
                    _snackbarEvents.tryEmit(msg)
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to record partial payment")
                },
            )
        }
    }

    fun confirmAdvancePaymentMonths(
        startMonth: PaymentMonth,
        member: MemberPaymentStatus,
        monthsCount: Int,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.confirmAdvancePaymentMonths(
                startMonth = startMonth,
                member = member,
                monthsCount = monthsCount,
                activeWhitelist = whitelist.value,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { monthIds ->
                    _snackbarEvents.tryEmit(
                        "Marked ${member.displayName} as Paid across ${monthIds.size} months (${monthIds.first()} → ${monthIds.last()})"
                    )
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to record advance payment")
                },
            )
        }
    }

    fun rejectMemberPayment(monthId: String, memberKey: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.rejectPaymentToPending(monthId, memberKey)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _receiptDialog.value = null
                    _snackbarEvents.tryEmit("Returned status to Pending for re-upload")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to reset payment")
                },
            )
        }
    }

    fun rewindMemberPayment(monthId: String, member: MemberPaymentStatus) {
        val targetStatus = if (member.hasReceipt) {
            AtaPotifyConstants.STATUS_VERIFYING
        } else {
            AtaPotifyConstants.STATUS_PENDING
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.rewindPayment(monthId, member.memberKey, targetStatus)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _receiptDialog.value = null
                    val label = if (targetStatus == AtaPotifyConstants.STATUS_VERIFYING) "Verifying" else "Pending"
                    _snackbarEvents.tryEmit("Rewound ${member.displayName}'s payment back to $label")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to rewind payment")
                },
            )
        }
    }

    fun setMemberPaymentStatusOverride(
        monthId: String,
        member: MemberPaymentStatus,
        targetStatus: String,
    ) {
        if (member.status == targetStatus) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = if (targetStatus == AtaPotifyConstants.STATUS_PAID) {
                paymentRepository.confirmPayment(monthId, member.memberKey)
            } else {
                paymentRepository.rewindPayment(monthId, member.memberKey, targetStatus)
            }
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val label = when (targetStatus) {
                        AtaPotifyConstants.STATUS_PAID -> "Paid"
                        AtaPotifyConstants.STATUS_VERIFYING -> "Verifying"
                        else -> "Pending"
                    }
                    _snackbarEvents.tryEmit("Marked ${member.displayName} as $label")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update payment status")
                },
            )
        }
    }

    fun editMemberPayment(
        monthId: String,
        member: MemberPaymentStatus,
        displayName: String,
        status: String,
        amountLkr: Long,
        clearReceipt: Boolean,
        resetReminders: Boolean,
        familyGroup: String? = null,
        paidAmountLkr: Long = member.paidAmountLkr,
        paymentMethod: String = member.paymentMethod,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.editMemberPayment(
                monthId = monthId,
                member = member,
                newDisplayName = displayName,
                newStatus = status,
                newAmountLkr = amountLkr,
                clearReceipt = clearReceipt,
                resetReminders = resetReminders,
                familyGroup = familyGroup,
                newPaidAmountLkr = paidAmountLkr,
                newPaymentMethod = paymentMethod,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Updated ${displayName.trim()}'s payment log")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update payment log")
                },
            )
        }
    }

    fun deleteMemberPayment(
        monthId: String,
        member: MemberPaymentStatus,
        monthMembers: List<MemberPaymentStatus>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.deleteMemberPayment(
                monthId = monthId,
                member = member,
                allMonthMembers = monthMembers,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Removed ${member.displayName} from $monthId")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to remove payment entry")
                },
            )
        }
    }

    fun addMemberToMonth(
        month: PaymentMonth,
        existingMembers: List<MemberPaymentStatus>,
        email: String,
        displayName: String,
        amountLkr: Long,
        familyGroup: String = AtaPotifyConstants.GROUP_1,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.addMemberToMonth(
                month = month,
                existingMembers = existingMembers,
                email = email,
                displayName = displayName,
                amountLkr = amountLkr,
                familyGroup = familyGroup,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Added $displayName to ${month.displayLabel}")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to add member to month")
                },
            )
        }
    }

    fun deleteBillingMonth(
        month: PaymentMonth,
        members: List<MemberPaymentStatus>,
        stats: List<ListeningStat>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.deleteBillingMonth(
                monthId = month.monthId,
                memberKeys = members.map { it.memberKey },
                statKeys = stats.map { it.memberKey },
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    if (_selectedHistoryMonthId.value == month.monthId) {
                        _selectedHistoryMonthId.value = null
                    }
                    _snackbarEvents.tryEmit("Deleted ${month.displayLabel}")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to delete billing month")
                },
            )
        }
    }

    fun sendWhatsAppReminder(
        context: Context,
        month: PaymentMonth,
        member: MemberPaymentStatus,
        phoneDigits: String,
        customMessage: String? = null,
    ) {
        val cleanPhone = AtaPotifyConstants.sanitizeWhatsAppDigits(phoneDigits)
        if (cleanPhone.length < 9) {
            _snackbarEvents.tryEmit("No valid WhatsApp number saved for ${member.displayName}")
            return
        }
        val text = customMessage ?: buildStandardWhatsAppMessage(
            member = member,
            monthLabel = month.displayLabel,
        )
        val url = "https://wa.me/$cleanPhone?text=${URLEncoder.encode(text, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onSuccess {
            viewModelScope.launch {
                paymentRepository.recordWhatsAppReminder(
                    monthId = month.monthId,
                    memberKey = member.memberKey,
                    currentCount = member.reminderCount,
                )
            }
        }.onFailure {
            _snackbarEvents.tryEmit("Unable to open WhatsApp or browser for $cleanPhone")
        }
    }

    fun shareMonthlySummaryToWhatsApp(
        context: Context,
        month: PaymentMonth,
        members: List<MemberPaymentStatus>,
        stats: List<ListeningStat>,
        groupFilter: String = _selectedGroupFilter.value,
    ) {
        val text = buildWhatsAppMonthlySummaryText(
            month = month,
            members = members,
            stats = stats,
            planConfig = planConfig.value,
            groupFilter = groupFilter,
            familyGroups = familyGroups.value,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AtaPotify — ${month.displayLabel}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${month.displayLabel} Summary").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(chooser)
        }.onFailure {
            _snackbarEvents.tryEmit("Unable to open share sheet")
        }
    }

    fun startNewMonth(monthId: String, displayLabel: String, amountLkr: Long, onDone: () -> Unit) {
        val activeMembers = whitelist.value.filter { it.active }
        if (activeMembers.isEmpty()) {
            _snackbarEvents.tryEmit("Add at least one active friend to the Whitelist first.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.startNewMonth(
                monthId = monthId.trim(),
                displayLabel = displayLabel.trim().ifBlank { AtaPotifyConstants.monthLabelFromId(monthId.trim()) },
                amountLkr = amountLkr,
                activeMembers = activeMembers,
                familyGroups = familyGroups.value,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Started $displayLabel for ${activeMembers.size} friends")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to start new month")
                },
            )
        }
    }

    fun updateMonthlyLkrRate(newAmountLkr: Long, alsoUpdateActiveMonth: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val activeMonth = if (alsoUpdateActiveMonth) currentMonth.value else null
            val emails = whitelist.value.filter { it.active }.map { AtaPotifyConstants.normalizeEmail(it.email) }
            val memberKeys = currentMonthMembers.value.map { it.memberKey }
            val res = paymentRepository.updateMonthlyAmountLkr(
                newAmountLkr = newAmountLkr,
                alsoUpdateMonthId = activeMonth?.monthId,
                existingAllowedEmails = emails,
                monthMemberKeys = memberKeys,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { _snackbarEvents.tryEmit("Monthly rate updated to LKR $newAmountLkr") },
                onFailure = { e -> _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update LKR rate") },
            )
        }
    }

    fun updateBankDetails(
        bankName: String,
        bankAccountNumber: String,
        bankAccountHolder: String,
        bankBranch: String,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val emails = whitelist.value.filter { it.active }.map { AtaPotifyConstants.normalizeEmail(it.email) }
            val res = paymentRepository.updateBankDetails(
                bankName = bankName,
                bankAccountNumber = bankAccountNumber,
                bankAccountHolder = bankAccountHolder,
                bankBranch = bankBranch,
                existingAllowedEmails = emails,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { _snackbarEvents.tryEmit("Bank transfer details saved") },
                onFailure = { e -> _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save bank details") },
            )
        }
    }

    fun updatePinnedAnnouncement(announcement: String) {
        val clean = announcement.trim()
        updatePinnedAnnouncements(if (clean.isEmpty()) emptyList() else listOf(clean))
    }

    fun updatePinnedAnnouncements(announcements: List<String>) {
        viewModelScope.launch {
            _isBusy.value = true
            val emails = whitelist.value.filter { it.active }.map { AtaPotifyConstants.normalizeEmail(it.email) }
            val cleanList = announcements.map { it.trim().take(280) }.filter { it.isNotEmpty() }
                .take(AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS)
            val res = paymentRepository.updatePinnedAnnouncements(
                announcements = cleanList,
                existingAllowedEmails = emails,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val msg = when {
                        cleanList.isEmpty() -> "Cleared all pinned announcements"
                        cleanList.size == 1 -> "Pinned announcement to Board"
                        else -> "Updated ${cleanList.size} pinned slideshow announcements"
                    }
                    _snackbarEvents.tryEmit(msg)
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update announcements")
                },
            )
        }
    }

    fun addPinnedAnnouncement(newText: String) {
        val clean = newText.trim().take(280)
        if (clean.isEmpty()) return
        val current = planConfig.value.effectiveAnnouncements
        if (current.size >= AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS) {
            _snackbarEvents.tryEmit("Maximum ${AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS} pinned messages reached")
            return
        }
        updatePinnedAnnouncements(current + clean)
    }

    fun removePinnedAnnouncementAt(index: Int) {
        val current = planConfig.value.effectiveAnnouncements.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        updatePinnedAnnouncements(current)
    }

    fun shareVerifiedReceiptToWhatsApp(
        context: Context,
        month: PaymentMonth,
        member: MemberPaymentStatus,
    ) {
        val streak = _paymentStreaks.value[member.memberKey] ?: 0
        val text = buildVerifiedPaymentReceiptText(
            month = month,
            member = member,
            streakCount = streak,
            planName = planConfig.value.planName,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AtaPotify Receipt — ${month.displayLabel}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Verified Receipt").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(chooser)
        }.onFailure {
            _snackbarEvents.tryEmit("Unable to open share sheet")
        }
    }

    fun openInSpotifySearch(context: Context, query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        val spotifyUriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webFallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(spotifyUriIntent)
        }.recoverCatching {
            context.startActivity(webFallbackIntent)
        }.onFailure {
            _snackbarEvents.tryEmit("Unable to open Spotify for \"$cleanQuery\"")
        }
    }

    fun upsertWhitelistMember(
        email: String,
        displayName: String,
        phone: String,
        active: Boolean,
        spotifyEmail: String? = null,
        familyGroup: String? = null,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val resolvedGroupId = familyGroup?.let { AtaPotifyConstants.normalizeFamilyGroup(it) }
            val groupRate = resolvedGroupId?.let { id ->
                familyGroups.value.firstOrNull { it.effectiveId == id }?.amountLkr
            }
            val res = paymentRepository.upsertWhitelistMember(
                email = email,
                displayName = displayName,
                phone = phone,
                active = active,
                allCurrentWhitelist = whitelist.value,
                activeMonth = currentMonth.value,
                activeMonthMembers = currentMonthMembers.value,
                spotifyEmail = spotifyEmail,
                familyGroup = familyGroup,
                groupAmountLkr = groupRate,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val groupLabel = familyGroup?.let {
                        " (${AtaPotifyConstants.familyGroupLabel(it, familyGroups.value)})"
                    }.orEmpty()
                    _snackbarEvents.tryEmit("Saved $displayName$groupLabel")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save friend")
                },
            )
        }
    }

    fun removeWhitelistMember(memberKey: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.removeWhitelistMember(memberKey, whitelist.value)
            _isBusy.value = false
            res.fold(
                onSuccess = { _snackbarEvents.tryEmit("Removed member from approved roster") },
                onFailure = { e -> _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to remove member") },
            )
        }
    }

    fun approveAccessRequest(
        request: AccessRequest,
        familyGroup: String = AtaPotifyConstants.GROUP_1,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val resolvedGroupId = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)
            val groupRate = familyGroups.value.firstOrNull { it.effectiveId == resolvedGroupId }?.amountLkr
            val res = paymentRepository.approveAccessRequest(
                request = request,
                allCurrentWhitelist = whitelist.value,
                activeMonth = currentMonth.value,
                activeMonthMembers = currentMonthMembers.value,
                familyGroup = resolvedGroupId,
                groupAmountLkr = groupRate,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val groupLabel = AtaPotifyConstants.familyGroupLabel(resolvedGroupId, familyGroups.value)
                    _snackbarEvents.tryEmit("Approved ${request.displayName} into $groupLabel")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to approve request")
                },
            )
        }
    }

    fun saveFamilyGroup(
        groupId: String?,
        name: String,
        startDate: String,
        amountLkr: Long,
        colorHex: String,
        maxSlots: Long,
        alsoUpdateActiveMonthMembers: Boolean,
        address: String = "",
        managerName: String = "",
        managerEmail: String = "",
        inviteLink: String = "",
        notes: String = "",
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.saveFamilyGroup(
                groupId = groupId,
                name = name,
                startDate = startDate,
                amountLkr = amountLkr,
                colorHex = colorHex,
                maxSlots = maxSlots,
                existingGroups = familyGroups.value,
                alsoUpdateActiveMonthMembers = alsoUpdateActiveMonthMembers,
                activeMonth = currentMonth.value,
                activeMonthMembers = currentMonthMembers.value,
                address = address,
                managerName = managerName,
                managerEmail = managerEmail,
                inviteLink = inviteLink,
                notes = notes,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val action = if (groupId.isNullOrBlank()) "Created" else "Updated"
                    _snackbarEvents.tryEmit("$action ${name.trim()} (LKR $amountLkr)")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save family group")
                },
            )
        }
    }

    fun deleteFamilyGroup(
        group: FamilyGroup,
        reassignToGroupId: String?,
        onDone: () -> Unit = {},
    ) {
        if (familyGroups.value.size <= 1) {
            _snackbarEvents.tryEmit("At least one Family Group must remain.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.deleteFamilyGroup(
                groupId = group.effectiveId,
                reassignToGroupId = reassignToGroupId,
                whitelist = whitelist.value,
                activeMonth = currentMonth.value,
                activeMonthMembers = currentMonthMembers.value,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    if (_selectedGroupFilter.value == group.effectiveId) {
                        _selectedGroupFilter.value = AtaPotifyConstants.GROUP_ALL
                    }
                    _snackbarEvents.tryEmit("Deleted ${group.name}")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to delete family group")
                },
            )
        }
    }

    fun transferMemberToGroup(
        memberKey: String,
        memberName: String,
        targetGroupId: String,
        updateActiveMonthAmount: Boolean = true,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val targetGroup = familyGroups.value.firstOrNull { it.effectiveId == targetGroupId }
            val res = paymentRepository.transferMemberToGroup(
                memberKey = memberKey,
                targetGroupId = targetGroupId,
                targetGroupAmountLkr = targetGroup?.amountLkr,
                updateActiveMonthAmount = updateActiveMonthAmount,
                activeMonth = currentMonth.value,
                activeMonthMembers = currentMonthMembers.value,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    val targetLabel = AtaPotifyConstants.familyGroupLabel(targetGroupId, familyGroups.value)
                    _snackbarEvents.tryEmit("Moved $memberName to $targetLabel")
                    onDone()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to move member")
                },
            )
        }
    }

    fun declineAccessRequest(request: AccessRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = paymentRepository.declineAccessRequest(request)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Declined ${request.displayName}'s request")
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to decline request")
                },
            )
        }
    }

    fun launchSpotifyConnect(context: Context) {
        spotifyService.buildAuthorizationIntent().fold(
            onSuccess = { intent ->
                runCatching { context.startActivity(intent) }
                    .onFailure { _snackbarEvents.tryEmit("Unable to open browser for Spotify login") }
            },
            onFailure = { e ->
                _snackbarEvents.tryEmit(e.message ?: "Spotify setup incomplete")
            },
        )
    }

    fun handleSpotifyCallback(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val tokenRes = spotifyService.handleRedirectUri(uri)
            _isBusy.value = false
            tokenRes.fold(
                onSuccess = {
                    _spotifyConnected.value = true
                    SpotifySyncScheduler.schedulePeriodicSync(appContext)
                    _snackbarEvents.tryEmit("Spotify connected! Background sync enabled & syncing stats...")
                    syncSpotifyStats()
                },
                onFailure = { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Spotify login failed")
                },
            )
        }
    }

    fun syncSpotifyStats(silent: Boolean = false) {
        val month = currentMonth.value
        if (month == null) {
            if (!silent) {
                _snackbarEvents.tryEmit("No active billing month yet to attach listening stats.")
            }
            return
        }
        viewModelScope.launch {
            if (!silent) _isBusy.value = true
            val existingRemoteStat = currentMonthStats.value.firstOrNull { it.memberKey == session.memberKey }
            if (existingRemoteStat != null) {
                spotifyService.ensureMonthBaselineFromRemote(
                    monthId = month.monthId,
                    remoteMinutes = existingRemoteStat.minutesListened,
                    remoteTracks = existingRemoteStat.tracksCounted,
                )
            }
            val statsRes = spotifyService.fetchAndAccumulateMonthlyStats(month.monthId)
            statsRes.fold(
                onSuccess = { snap ->
                    _spotifyConnected.value = true
                    _spotifyName.value = snap.spotifyDisplayName
                    val displayName = userProfile.value?.displayName?.takeIf { it.isNotBlank() } ?: session.displayName
                    val writeRes = paymentRepository.syncListeningStats(
                        monthId = month.monthId,
                        memberKey = session.memberKey,
                        memberEmail = session.email,
                        displayName = displayName,
                        spotifyDisplayName = snap.spotifyDisplayName,
                        minutesListened = snap.minutesListened,
                        tracksCounted = snap.tracksCounted,
                        topArtist = snap.topArtist,
                        topTrack = snap.topTrack,
                        allowedEmails = month.allowedEmails,
                    )
                    writeRes.fold(
                        onSuccess = {
                            if (!silent) {
                                _snackbarEvents.tryEmit("Synced ${snap.minutesListened} min (${snap.tracksCounted} tracks) for ${month.displayLabel}")
                            }
                        },
                        onFailure = { e ->
                            if (!silent) {
                                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save Spotify stats")
                            }
                        },
                    )
                },
                onFailure = { e ->
                    if (!silent) {
                        _snackbarEvents.tryEmit(e.localizedMessage ?: "Unable to fetch Spotify stats")
                    }
                },
            )
            if (!silent) _isBusy.value = false
        }
    }

    fun disconnectSpotify() {
        spotifyService.disconnect()
        SpotifySyncScheduler.cancelPeriodicSync(appContext)
        _spotifyConnected.value = false
        _spotifyName.value = ""
        _snackbarEvents.tryEmit("Disconnected Spotify account on this device")
    }

    fun emitSnackbar(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    private fun buildStandardWhatsAppMessage(
        member: MemberPaymentStatus,
        monthLabel: String,
    ): String = if (member.hasPartialPayment) {
        "Hey ${member.displayName}! Friendly reminder for your AtaPotify (Spotify Family) share for $monthLabel: LKR ${member.remainingLkr} remaining (already paid LKR ${member.effectivePaidLkr} of LKR ${member.amountLkr}). Please settle when you can!"
    } else {
        "Hey ${member.displayName}! Friendly reminder for your AtaPotify (Spotify Family) share for $monthLabel: LKR ${member.amountLkr}. Please upload your payment slip in the app once sent."
    }

    private fun buildOverdueWhatsAppMessage(
        member: MemberPaymentStatus,
        monthLabel: String,
        daysPending: Long,
    ): String = if (member.hasPartialPayment) {
        "Hey ${member.displayName}! Your AtaPotify (Spotify Family) balance of LKR ${member.remainingLkr} (paid LKR ${member.effectivePaidLkr} of LKR ${member.amountLkr}) for $monthLabel has been pending for $daysPending days. Please settle the balance when you get a chance!"
    } else {
        "Hey ${member.displayName}! Your AtaPotify (Spotify Family) payment of LKR ${member.amountLkr} for $monthLabel has been pending for $daysPending days. Please settle it and upload your slip in AtaPotify when you get a chance!"
    }

    private companion object {
        const val TAG = "AtaPotifyVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}

