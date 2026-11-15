package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.WhitelistMember
import com.example.data.repository.AccessResolution
import com.example.data.repository.AuthRepository
import com.example.data.repository.PaymentRepository
import com.example.data.repository.authStateFlow
import com.example.data.service.SpotifyStatsService
import com.example.data.worker.BillingReminderNotifier
import com.example.data.worker.BillingReminderScheduler
import com.example.data.worker.SpotifySyncScheduler
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.screens.AccessDeniedScreen
import com.example.ui.screens.AddMemberToMonthDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AdvancePaymentDialog
import com.example.ui.screens.AppUpdateAvailableDialog
import com.example.ui.screens.AutoReminderQueueSheet
import com.example.ui.screens.AwaitingApprovalScreen
import com.example.ui.screens.BoardScreen
import com.example.ui.screens.ConfirmDeleteDialog
import com.example.ui.screens.EditPaymentDialog
import com.example.ui.screens.FamilyGroupsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoadingGateScreen
import com.example.ui.screens.ProfileEditorSheet
import com.example.ui.screens.ReceiptSlipDialog
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SpotifyEmailOnboardingDialog
import com.example.ui.screens.SpotifySetupHintDialog
import com.example.ui.screens.StartNewMonthDialog
import com.example.ui.screens.VerifiedPaymentReceiptDialog
import com.example.ui.screens.WhitelistMemberDialog
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.screens.signOutFromApp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AtaPotifyTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AtaPotifyViewModel
import com.example.ui.viewmodel.AuthGateUiState
import com.example.ui.viewmodel.AuthGateViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private enum class AppTab(
    val routeId: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    BOARD("board", R.string.nav_board, Icons.Default.Dashboard),
    HISTORY("history", R.string.nav_history, Icons.Default.History),
    GROUPS("groups", R.string.nav_groups, Icons.Default.Groups),
    ADMIN("admin", R.string.nav_admin, Icons.Default.AdminPanelSettings),
}

class MainActivity : ComponentActivity() {

    private val pendingSpotifyCallback = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureSpotifyCallbackIntent(intent)

        setContent {
            AtaPotifyTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember(context) { CredentialManager.create(context) }
                val currentUser by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
                val pendingCallbackUri by pendingSpotifyCallback.asStateFlow()
                    .collectAsStateWithLifecycle()

                var hasAttemptedSilentSignIn by remember { mutableStateOf(false) }

                LaunchedEffect(currentUser) {
                    if (currentUser == null && !hasAttemptedSilentSignIn) {
                        hasAttemptedSilentSignIn = true
                        attemptAutoSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {},
                            onUnauthenticated = {},
                            scope = scope,
                        )
                    }
                }

                AnimatedContent(
                    targetState = currentUser,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                    },
                    label = "root_auth_transition",
                ) { user ->
                    if (user == null) {
                        SignInScreen(
                            onAuthSuccess = {},
                        )
                    } else {
                        AuthGatedHost(
                            firebaseUser = user,
                            pendingSpotifyUri = pendingCallbackUri,
                            onConsumeSpotifyUri = { pendingSpotifyCallback.value = null },
                            onSignOut = {
                                SpotifySyncScheduler.cancelPeriodicSync(context)
                                BillingReminderScheduler.cancelPeriodicChecks(context)
                                signOutFromApp(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureSpotifyCallbackIntent(intent)
    }

    private fun captureSpotifyCallbackIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "atapotify" && uri.host == "spotify-callback") {
            pendingSpotifyCallback.value = uri
        }
    }
}

@Composable
private fun AuthGatedHost(
    firebaseUser: FirebaseUser,
    pendingSpotifyUri: Uri?,
    onConsumeSpotifyUri: () -> Unit,
    onSignOut: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val databaseId = remember(appContext) {
        appContext.getString(R.string.firestore_database_id)
    }
    val firestore = remember(databaseId) {
        FirebaseFirestore.getInstance(databaseId)
    }
    val authRepository = remember(firestore) {
        AuthRepository(db = firestore, auth = Firebase.auth)
    }
    val paymentRepository = remember(firestore) {
        PaymentRepository(db = firestore, auth = Firebase.auth)
    }
    val spotifyService = remember(appContext) {
        SpotifyStatsService(appContext)
    }

    val gateViewModel: AuthGateViewModel = viewModel(
        key = "gate_${firebaseUser.uid}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthGateViewModel(
                    authRepository = authRepository,
                    firebaseUser = firebaseUser,
                ) as T
            }
        },
    )

    val gateState by gateViewModel.authGateState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = gateState,
        transitionSpec = {
            fadeIn(tween(320)) togetherWith fadeOut(tween(220))
        },
        contentKey = { it::class },
        label = "auth_gate_transition",
    ) { state ->
        when (state) {
            AuthGateUiState.CheckingAccess -> {
                LoadingGateScreen(message = "Verifying family plan access...")
            }

            is AuthGateUiState.AwaitingApproval -> {
                AwaitingApprovalScreen(
                    info = state.info,
                    isSavingDetails = state.isSavingDetails,
                    infoMessage = state.infoMessage,
                    onSaveDetails = { name, phone, spotifyEmail ->
                        gateViewModel.updatePendingDetails(name, phone, spotifyEmail)
                    },
                    onRetry = { gateViewModel.verifyAccess() },
                    onSignOut = onSignOut,
                )
            }

            is AuthGateUiState.Denied -> {
                AccessDeniedScreen(
                    email = state.email,
                    errorMessage = state.errorMessage,
                    onRetry = { gateViewModel.verifyAccess() },
                    onSignOut = onSignOut,
                )
            }

            is AuthGateUiState.Authorized -> {
                val mainViewModel: AtaPotifyViewModel = viewModel(
                    key = "main_${state.session.uid}_${state.session.isAdmin}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AtaPotifyViewModel(
                                appContext = appContext,
                                authRepository = authRepository,
                                paymentRepository = paymentRepository,
                                spotifyService = spotifyService,
                                session = state.session,
                            ) as T
                        }
                    },
                )

                MainAppScaffold(
                    session = state.session,
                    viewModel = mainViewModel,
                    pendingSpotifyUri = pendingSpotifyUri,
                    onConsumeSpotifyUri = onConsumeSpotifyUri,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(
    session: AccessResolution.Authorized,
    viewModel: AtaPotifyViewModel,
    pendingSpotifyUri: Uri?,
    onConsumeSpotifyUri: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val planConfig by viewModel.planConfig.collectAsStateWithLifecycle()
    val familyGroups by viewModel.familyGroups.collectAsStateWithLifecycle()
    val whitelist by viewModel.whitelist.collectAsStateWithLifecycle()
    val pendingAccessRequests by viewModel.pendingAccessRequests.collectAsStateWithLifecycle()
    val paymentMonths by viewModel.paymentMonths.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedHistoryMonth by viewModel.selectedHistoryMonth.collectAsStateWithLifecycle()
    val currentMonthMembers by viewModel.currentMonthMembers.collectAsStateWithLifecycle()
    val currentMonthStats by viewModel.currentMonthStats.collectAsStateWithLifecycle()
    val historyMonthMembers by viewModel.historyMonthMembers.collectAsStateWithLifecycle()
    val historyMonthStats by viewModel.historyMonthStats.collectAsStateWithLifecycle()
    val overdueReminders by viewModel.overdueReminders.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val receiptDialogState by viewModel.receiptDialog.collectAsStateWithLifecycle()
    val showAutoReminderSheet by viewModel.showAutoReminderSheet.collectAsStateWithLifecycle()
    val spotifyConnected by viewModel.spotifyConnected.collectAsStateWithLifecycle()
    val spotifyName by viewModel.spotifyName.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val needsSpotifyEmailPrompt by viewModel.needsSpotifyEmailPrompt.collectAsStateWithLifecycle()
    val allTimeWrapped by viewModel.allTimeWrapped.collectAsStateWithLifecycle()
    val paymentStreaks by viewModel.paymentStreaks.collectAsStateWithLifecycle()
    val selectedGroupFilter by viewModel.selectedGroupFilter.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.BOARD) }
    var notificationsEnabled by remember {
        mutableStateOf(BillingReminderNotifier.hasNotificationPermission(context))
    }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showStartMonthDialog by remember { mutableStateOf(false) }
    var showSpotifyHintDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var editingWhitelistMember by remember { mutableStateOf<WhitelistMember?>(null) }
    var editingPaymentContext by remember { mutableStateOf<Pair<PaymentMonth, MemberPaymentStatus>?>(null) }
    var advancePaymentContext by remember { mutableStateOf<Pair<PaymentMonth, MemberPaymentStatus>?>(null) }
    var verifiedReceiptContext by remember { mutableStateOf<Pair<PaymentMonth, MemberPaymentStatus>?>(null) }
    var deletingPaymentContext by remember { mutableStateOf<Triple<PaymentMonth, MemberPaymentStatus, List<MemberPaymentStatus>>?>(null) }
    var addMemberTargetContext by remember { mutableStateOf<Pair<PaymentMonth, List<MemberPaymentStatus>>?>(null) }
    var deletingMonthContext by remember { mutableStateOf<Triple<PaymentMonth, List<MemberPaymentStatus>, List<ListeningStat>>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted || BillingReminderNotifier.hasNotificationPermission(context)
        if (granted) {
            viewModel.notifyRemindersPermissionGranted()
            viewModel.emitSnackbar("Billing cycle reminders enabled")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BillingReminderNotifier.hasNotificationPermission(context)
        ) {
            val prefs = context.getSharedPreferences("atapotify_ui_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked_post_notifications", false)) {
                prefs.edit().putBoolean("asked_post_notifications", true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val copyToClipboard: (String, String) -> Unit = remember(context, viewModel) {
        { label, text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            viewModel.emitSnackbar("Copied $label to clipboard")
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = BillingReminderNotifier.hasNotificationPermission(context)
                viewModel.autoSyncSpotifyOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tabs = remember(session.isAdmin) {
        if (session.isAdmin) listOf(AppTab.BOARD, AppTab.HISTORY, AppTab.GROUPS, AppTab.ADMIN)
        else listOf(AppTab.BOARD, AppTab.HISTORY)
    }

    LaunchedEffect(session.isAdmin) {
        if (!session.isAdmin && (selectedTab == AppTab.ADMIN || selectedTab == AppTab.GROUPS)) {
            selectedTab = AppTab.BOARD
        }
    }

    LaunchedEffect(pendingSpotifyUri) {
        if (pendingSpotifyUri != null) {
            viewModel.handleSpotifyCallback(pendingSpotifyUri)
            onConsumeSpotifyUri()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = selectedTab != AppTab.BOARD) {
        selectedTab = AppTab.BOARD
    }

    val effectiveDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: session.displayName
    val adminDisplayName = if (session.isAdmin) effectiveDisplayName else "Admin"
    val phoneByKey = remember(whitelist) { whitelist.associate { it.memberKey to it.phone } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SurfaceDark,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (spotifyConnected) {
                                        AnimatedEqualizerBars(isActive = true)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = stringResource(R.string.cd_app_emblem),
                                            tint = SpotifyEmerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val selfRoleLabel = if (session.isAdmin) {
                                        "Admin • ${familyGroups.size} Group(s)"
                                    } else {
                                        val wlGroup = whitelist
                                            .firstOrNull { it.memberKey == session.memberKey }
                                            ?.normalizedGroup
                                        val selfGroup = wlGroup ?: currentMonthMembers
                                            .firstOrNull { it.memberKey == session.memberKey }
                                            ?.normalizedGroup ?: session.familyGroup
                                        AtaPotifyConstants.familyGroupLabel(selfGroup, familyGroups)
                                    }
                                    Text(
                                        text = "$effectiveDisplayName • $selfRoleLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        actions = {
                            if (session.isAdmin && overdueReminders.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.openAutoReminderSheet() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("topbar_overdue_button"),
                                ) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = AmberWarning, contentColor = ObsidianBg) {
                                                Text(overdueReminders.size.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = stringResource(R.string.cd_overdue_alert),
                                            tint = AmberWarning,
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showProfileSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_profile_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.cd_profile),
                                    tint = SpotifyEmerald,
                                )
                            }
                        },
                    )
                    if (isBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = SpotifyEmerald,
                            trackColor = SurfaceDark,
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = SurfaceDark,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab
                            val badgeCount = if (tab == AppTab.ADMIN) pendingAccessRequests.size else 0
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = AmberWarning, contentColor = ObsidianBg) {
                                                    Text(badgeCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes),
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.routeId}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyEmerald,
                                    selectedTextColor = SpotifyEmerald,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = SurfaceDark,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(Modifier.size(12.dp))
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab
                            val badgeCount = if (tab == AppTab.ADMIN) pendingAccessRequests.size else 0
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = AmberWarning, contentColor = ObsidianBg) {
                                                    Text(badgeCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes),
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("rail_${tab.routeId}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SpotifyEmerald,
                                    selectedTextColor = SpotifyEmerald,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(260)) { it / 6 * direction } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 6 * direction } + fadeOut(tween(200)))
                    },
                    label = "main_tab_transition",
                ) { tab ->
                    when (tab) {
                        AppTab.BOARD -> BoardScreen(
                            session = session,
                            adminDisplayName = adminDisplayName,
                            currentMonth = currentMonth,
                            members = currentMonthMembers,
                            stats = currentMonthStats,
                            whitelist = whitelist,
                            familyGroups = familyGroups,
                            overdueReminders = overdueReminders,
                            defaultAmountLkr = planConfig.monthlyAmountLkr,
                            planConfig = planConfig,
                            paymentStreaks = paymentStreaks,
                            selectedGroupFilter = selectedGroupFilter,
                            onSelectGroupFilter = { viewModel.selectGroupFilter(it) },
                            spotifyConnected = spotifyConnected,
                            spotifyName = spotifyName,
                            isSpotifyClientConfigured = viewModel.isSpotifyClientConfigured,
                            isBusy = isBusy,
                            onOpenStartMonth = { showStartMonthDialog = true },
                            onOpenAddMemberToMonth = {
                                currentMonth?.let { month ->
                                    addMemberTargetContext = month to currentMonthMembers
                                }
                            },
                            onDeleteCurrentMonth = {
                                currentMonth?.let { month ->
                                    deletingMonthContext = Triple(month, currentMonthMembers, currentMonthStats)
                                }
                            },
                            onUploadSlip = { uri, member ->
                                currentMonth?.let { month ->
                                    viewModel.uploadReceiptSlip(context, uri, month.monthId, member)
                                }
                            },
                            onViewSlip = { member ->
                                currentMonth?.let { month ->
                                    viewModel.openReceiptViewer(month.monthId, member)
                                }
                            },
                            onOpenVerifiedReceipt = { member ->
                                currentMonth?.let { month ->
                                    verifiedReceiptContext = month to member
                                }
                            },
                            onConfirmPaid = { member ->
                                currentMonth?.let { month ->
                                    viewModel.confirmMemberPayment(month.monthId, member.memberKey)
                                }
                            },
                            onAdvancePayClick = { member ->
                                currentMonth?.let { month ->
                                    advancePaymentContext = month to member
                                }
                            },
                            onRewindPaid = { member ->
                                currentMonth?.let { month ->
                                    viewModel.rewindMemberPayment(month.monthId, member)
                                }
                            },
                            onEditPayment = { member ->
                                currentMonth?.let { month ->
                                    editingPaymentContext = month to member
                                }
                            },
                            onDeletePayment = { member ->
                                currentMonth?.let { month ->
                                    deletingPaymentContext = Triple(month, member, currentMonthMembers)
                                }
                            },
                            onRemindMember = { member, phone ->
                                currentMonth?.let { month ->
                                    viewModel.sendWhatsAppReminder(context, month, member, phone)
                                }
                            },
                            onCopyText = copyToClipboard,
                            onRemoveAnnouncementSlide = { idx -> viewModel.removePinnedAnnouncementAt(idx) },
                            onClearAnnouncement = { viewModel.updatePinnedAnnouncements(emptyList()) },
                            onShareMonthlySummary = {
                                currentMonth?.let { month ->
                                    val groupByKey = buildMap {
                                        currentMonthMembers.forEach { put(it.memberKey, it.normalizedGroup) }
                                        whitelist.forEach { put(it.memberKey, it.normalizedGroup) }
                                    }
                                    val filteredMembers = if (selectedGroupFilter == AtaPotifyConstants.GROUP_ALL) {
                                        currentMonthMembers
                                    } else {
                                        currentMonthMembers.filter {
                                            (groupByKey[it.memberKey] ?: it.normalizedGroup) == selectedGroupFilter
                                        }
                                    }
                                    viewModel.shareMonthlySummaryToWhatsApp(
                                        context = context,
                                        month = month,
                                        members = filteredMembers,
                                        stats = currentMonthStats,
                                        groupFilter = selectedGroupFilter,
                                    )
                                }
                            },
                            onOpenOverdueQueue = { viewModel.openAutoReminderSheet() },
                            onConnectSpotify = { viewModel.launchSpotifyConnect(context) },
                            onSyncSpotify = { viewModel.syncSpotifyStats() },
                            onShowSpotifySetupHint = { showSpotifyHintDialog = true },
                            onOpenSpotifyQuery = { query -> viewModel.openInSpotifySearch(context, query) },
                        )

                        AppTab.HISTORY -> HistoryScreen(
                            session = session,
                            adminDisplayName = adminDisplayName,
                            months = paymentMonths,
                            selectedMonth = selectedHistoryMonth,
                            members = historyMonthMembers,
                            stats = historyMonthStats,
                            whitelist = whitelist,
                            familyGroups = familyGroups,
                            paymentStreaks = paymentStreaks,
                            allTimeWrapped = allTimeWrapped,
                            selectedGroupFilter = selectedGroupFilter,
                            onSelectGroupFilter = { viewModel.selectGroupFilter(it) },
                            isBusy = isBusy,
                            onSelectMonth = { viewModel.selectHistoryMonth(it) },
                            onOpenAddMemberToMonth = { month ->
                                addMemberTargetContext = month to historyMonthMembers
                            },
                            onDeleteMonth = { month ->
                                deletingMonthContext = Triple(month, historyMonthMembers, historyMonthStats)
                            },
                            onShareMonthSummary = { month ->
                                val groupByKey = buildMap {
                                    historyMonthMembers.forEach { put(it.memberKey, it.normalizedGroup) }
                                    whitelist.forEach { if (!containsKey(it.memberKey)) put(it.memberKey, it.normalizedGroup) }
                                }
                                val filteredMembers = if (selectedGroupFilter == AtaPotifyConstants.GROUP_ALL) {
                                    historyMonthMembers
                                } else {
                                    historyMonthMembers.filter {
                                        (groupByKey[it.memberKey] ?: it.normalizedGroup) == selectedGroupFilter
                                    }
                                }
                                viewModel.shareMonthlySummaryToWhatsApp(
                                    context = context,
                                    month = month,
                                    members = filteredMembers,
                                    stats = historyMonthStats,
                                    groupFilter = selectedGroupFilter,
                                )
                            },
                            onViewSlip = { member ->
                                selectedHistoryMonth?.let { month ->
                                    viewModel.openReceiptViewer(month.monthId, member)
                                }
                            },
                            onOpenVerifiedReceipt = { member ->
                                selectedHistoryMonth?.let { month ->
                                    verifiedReceiptContext = month to member
                                }
                            },
                            onConfirmPaid = { member ->
                                selectedHistoryMonth?.let { month ->
                                    viewModel.confirmMemberPayment(month.monthId, member.memberKey)
                                }
                            },
                            onAdvancePayClick = { member ->
                                selectedHistoryMonth?.let { month ->
                                    advancePaymentContext = month to member
                                }
                            },
                            onRewindPaid = { member ->
                                selectedHistoryMonth?.let { month ->
                                    viewModel.rewindMemberPayment(month.monthId, member)
                                }
                            },
                            onQuickStatusOverride = { member, targetStatus ->
                                selectedHistoryMonth?.let { month ->
                                    viewModel.setMemberPaymentStatusOverride(month.monthId, member, targetStatus)
                                }
                            },
                            onEditPayment = { member ->
                                selectedHistoryMonth?.let { month ->
                                    editingPaymentContext = month to member
                                }
                            },
                            onDeletePayment = { member ->
                                selectedHistoryMonth?.let { month ->
                                    deletingPaymentContext = Triple(month, member, historyMonthMembers)
                                }
                            },
                            onRemindMember = { member, phone ->
                                selectedHistoryMonth?.let { month ->
                                    viewModel.sendWhatsAppReminder(context, month, member, phone)
                                }
                            },
                            onOpenSpotifyQuery = { query -> viewModel.openInSpotifySearch(context, query) },
                        )

                        AppTab.GROUPS -> FamilyGroupsScreen(
                            familyGroups = familyGroups,
                            whitelist = whitelist,
                            currentMonth = currentMonth,
                            defaultAmountLkr = planConfig.monthlyAmountLkr,
                            isBusy = isBusy,
                            onCreateOrUpdateGroup = { groupId, name, startDate, amountLkr, colorHex, maxSlots, updateActiveMonth, onDone ->
                                viewModel.saveFamilyGroup(
                                    groupId = groupId,
                                    name = name,
                                    startDate = startDate,
                                    amountLkr = amountLkr,
                                    colorHex = colorHex,
                                    maxSlots = maxSlots,
                                    alsoUpdateActiveMonthMembers = updateActiveMonth,
                                    onDone = onDone,
                                )
                            },
                            onDeleteGroup = { group, reassignToGroupId, onDone ->
                                viewModel.deleteFamilyGroup(
                                    group = group,
                                    reassignToGroupId = reassignToGroupId,
                                    onDone = onDone,
                                )
                            },
                            onTransferMember = { memberKey, memberName, targetGroupId, updateMonthAmount, onDone ->
                                viewModel.transferMemberToGroup(
                                    memberKey = memberKey,
                                    memberName = memberName,
                                    targetGroupId = targetGroupId,
                                    updateActiveMonthAmount = updateMonthAmount,
                                    onDone = onDone,
                                )
                            },
                            onAddNewMemberToGroup = { email, displayName, phone, spotifyEmail, targetGroupId, onDone ->
                                viewModel.upsertWhitelistMember(
                                    email = email,
                                    displayName = displayName,
                                    phone = phone,
                                    active = true,
                                    spotifyEmail = spotifyEmail,
                                    familyGroup = targetGroupId,
                                    onDone = onDone,
                                )
                            },
                        )

                        AppTab.ADMIN -> AdminScreen(
                            planConfig = planConfig,
                            currentMonth = currentMonth,
                            whitelist = whitelist,
                            familyGroups = familyGroups,
                            pendingRequests = pendingAccessRequests,
                            isBusy = isBusy,
                            onApproveRequest = { req, group -> viewModel.approveAccessRequest(req, group) },
                            onDeclineRequest = { viewModel.declineAccessRequest(it) },
                            onSaveLkrRate = { amount, alsoUpdateActive ->
                                viewModel.updateMonthlyLkrRate(amount, alsoUpdateActive)
                            },
                            onSaveBankDetails = { bankName, accNo, holder, branch ->
                                viewModel.updateBankDetails(bankName, accNo, holder, branch)
                            },
                            onSavePinnedAnnouncement = { text ->
                                viewModel.updatePinnedAnnouncement(text)
                            },
                            onAddPinnedAnnouncement = { text ->
                                viewModel.addPinnedAnnouncement(text)
                            },
                            onRemovePinnedAnnouncementAt = { idx ->
                                viewModel.removePinnedAnnouncementAt(idx)
                            },
                            onClearAllPinnedAnnouncements = {
                                viewModel.updatePinnedAnnouncements(emptyList())
                            },
                            onCopyText = copyToClipboard,
                            onAddMemberClick = {
                                editingWhitelistMember = null
                                showWhitelistDialog = true
                            },
                            onEditMemberClick = { member ->
                                editingWhitelistMember = member
                                showWhitelistDialog = true
                            },
                            onToggleMemberActive = { member, active ->
                                viewModel.upsertWhitelistMember(
                                    email = member.email,
                                    displayName = member.displayName,
                                    phone = member.phone,
                                    active = active,
                                    spotifyEmail = member.spotifyEmail,
                                    familyGroup = member.normalizedGroup,
                                    onDone = {},
                                )
                            },
                            onSwitchMemberGroup = { member, targetGroup ->
                                viewModel.transferMemberToGroup(
                                    memberKey = member.memberKey,
                                    memberName = member.displayName,
                                    targetGroupId = targetGroup,
                                    updateActiveMonthAmount = true,
                                    onDone = {},
                                )
                            },
                            onRemoveMemberClick = { member ->
                                viewModel.removeWhitelistMember(member.memberKey)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ProfileEditorSheet(
            session = session,
            profile = userProfile,
            spotifyConnected = spotifyConnected,
            spotifyName = spotifyName,
            notificationsEnabled = notificationsEnabled,
            onEnableNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            isBusy = isBusy,
            onSaveProfile = { newName, newPhone, newSpotifyEmail ->
                viewModel.updateOwnProfile(newName, newPhone, newSpotifyEmail) {
                    showProfileSheet = false
                }
            },
            onDisconnectSpotify = { viewModel.disconnectSpotify() },
            onCheckForUpdates = { viewModel.checkForAppUpdate(manual = true) },
            onSignOut = {
                showProfileSheet = false
                onSignOut()
            },
            onDismiss = { showProfileSheet = false },
        )
    }

    verifiedReceiptContext?.let { (month, member) ->
        val isSelf = member.memberEmail.equals(session.email, ignoreCase = true)
        VerifiedPaymentReceiptDialog(
            month = month,
            member = member,
            streakCount = paymentStreaks[member.memberKey] ?: 0,
            canViewSlip = (isSelf || session.isAdmin) && member.hasReceipt,
            onShareReceipt = {
                viewModel.shareVerifiedReceiptToWhatsApp(context, month, member)
            },
            onViewUploadedSlip = {
                verifiedReceiptContext = null
                viewModel.openReceiptViewer(month.monthId, member)
            },
            onDismiss = { verifiedReceiptContext = null },
        )
    }

    receiptDialogState?.let { dialogState ->
        ReceiptSlipDialog(
            state = dialogState,
            isAdmin = session.isAdmin,
            isBusy = isBusy,
            onConfirmPayment = {
                viewModel.confirmMemberPayment(dialogState.monthId, dialogState.member.memberKey)
            },
            onRejectToPending = {
                viewModel.rejectMemberPayment(dialogState.monthId, dialogState.member.memberKey)
            },
            onRewindPaid = {
                viewModel.rewindMemberPayment(dialogState.monthId, dialogState.member)
            },
            onDismiss = { viewModel.closeReceiptViewer() },
        )
    }

    if (showAutoReminderSheet && session.isAdmin) {
        AutoReminderQueueSheet(
            candidates = overdueReminders,
            onSendReminder = { candidate ->
                currentMonth?.let { month ->
                    viewModel.sendWhatsAppReminder(
                        context = context,
                        month = month,
                        member = candidate.member,
                        phoneDigits = phoneByKey[candidate.member.memberKey].orEmpty(),
                        customMessage = candidate.prefilledMessage,
                    )
                }
            },
            onDismiss = { viewModel.dismissAutoReminderSheet() },
        )
    }

    if (showStartMonthDialog && session.isAdmin) {
        val suggested = remember(paymentMonths) {
            AtaPotifyConstants.nextSuggestedMonthId(paymentMonths.map { it.monthId })
        }
        val activeCountByGroup = remember(whitelist, familyGroups) {
            val counts = mutableMapOf<String, Int>()
            familyGroups.forEach { counts[it.effectiveId] = 0 }
            whitelist.filter { it.active }.forEach { wl ->
                counts[wl.normalizedGroup] = (counts[wl.normalizedGroup] ?: 0) + 1
            }
            counts
        }
        StartNewMonthDialog(
            suggestedMonthId = suggested,
            defaultAmountLkr = planConfig.monthlyAmountLkr,
            activeMemberCount = whitelist.count { it.active },
            familyGroups = familyGroups,
            activeCountByGroup = activeCountByGroup,
            isBusy = isBusy,
            onConfirm = { monthId, label, amountLkr ->
                viewModel.startNewMonth(monthId, label, amountLkr) {
                    showStartMonthDialog = false
                }
            },
            onDismiss = { showStartMonthDialog = false },
        )
    }

    if (showWhitelistDialog && session.isAdmin) {
        val defaultGroup = remember(whitelist, familyGroups) {
            val counts = whitelist.filter { it.active }.groupingBy { it.normalizedGroup }.eachCount()
            familyGroups.firstOrNull { (counts[it.effectiveId] ?: 0) < it.maxSlots }?.effectiveId
                ?: familyGroups.firstOrNull()?.effectiveId
                ?: AtaPotifyConstants.GROUP_1
        }
        WhitelistMemberDialog(
            editingMember = editingWhitelistMember,
            familyGroups = familyGroups,
            defaultFamilyGroup = defaultGroup,
            isBusy = isBusy,
            onSave = { email, name, phone, spotifyEmail, active, familyGroup ->
                viewModel.upsertWhitelistMember(
                    email = email,
                    displayName = name,
                    phone = phone,
                    active = active,
                    spotifyEmail = spotifyEmail,
                    familyGroup = familyGroup,
                    onDone = {
                        showWhitelistDialog = false
                        editingWhitelistMember = null
                    },
                )
            },
            onDismiss = {
                showWhitelistDialog = false
                editingWhitelistMember = null
            },
        )
    }

    advancePaymentContext?.let { (month, member) ->
        if (session.isAdmin) {
            AdvancePaymentDialog(
                member = member,
                startMonth = month,
                isBusy = isBusy,
                onConfirmAdvance = { monthsCount ->
                    viewModel.confirmAdvancePaymentMonths(
                        startMonth = month,
                        member = member,
                        monthsCount = monthsCount,
                        onDone = { advancePaymentContext = null },
                    )
                },
                onDismiss = { advancePaymentContext = null },
            )
        }
    }

    editingPaymentContext?.let { (month, member) ->
        if (session.isAdmin) {
            val wlGroup = whitelist.firstOrNull { it.memberKey == member.memberKey }?.normalizedGroup
            EditPaymentDialog(
                member = member,
                monthLabel = month.displayLabel,
                familyGroups = familyGroups,
                initialFamilyGroup = wlGroup ?: member.normalizedGroup,
                isBusy = isBusy,
                onSave = { displayName, status, amountLkr, clearReceipt, resetReminders, familyGroup ->
                    viewModel.editMemberPayment(
                        monthId = month.monthId,
                        member = member,
                        displayName = displayName,
                        status = status,
                        amountLkr = amountLkr,
                        clearReceipt = clearReceipt,
                        resetReminders = resetReminders,
                        familyGroup = familyGroup,
                        onDone = { editingPaymentContext = null },
                    )
                },
                onDismiss = { editingPaymentContext = null },
            )
        }
    }

    addMemberTargetContext?.let { (month, existingMembers) ->
        if (session.isAdmin) {
            AddMemberToMonthDialog(
                month = month,
                existingMembers = existingMembers,
                whitelist = whitelist,
                familyGroups = familyGroups,
                adminDisplayName = adminDisplayName,
                isBusy = isBusy,
                onAdd = { email, displayName, amountLkr, familyGroup ->
                    viewModel.addMemberToMonth(
                        month = month,
                        existingMembers = existingMembers,
                        email = email,
                        displayName = displayName,
                        amountLkr = amountLkr,
                        familyGroup = familyGroup,
                        onDone = { addMemberTargetContext = null },
                    )
                },
                onDismiss = { addMemberTargetContext = null },
            )
        }
    }

    deletingPaymentContext?.let { (month, member, monthMembers) ->
        if (session.isAdmin) {
            ConfirmDeleteDialog(
                title = "Remove Payment Entry?",
                message = "Remove ${member.displayName} (${member.status.replaceFirstChar { it.uppercase() }}) from ${month.displayLabel}? Any uploaded slip for this entry will also be deleted.",
                confirmLabel = stringResource(R.string.btn_remove_payment),
                isBusy = isBusy,
                onConfirm = {
                    viewModel.deleteMemberPayment(
                        monthId = month.monthId,
                        member = member,
                        monthMembers = monthMembers,
                        onDone = { deletingPaymentContext = null },
                    )
                },
                onDismiss = { deletingPaymentContext = null },
            )
        }
    }

    deletingMonthContext?.let { (month, members, stats) ->
        if (session.isAdmin) {
            ConfirmDeleteDialog(
                title = "Delete Billing Month?",
                message = "Permanently delete ${month.displayLabel} and all ${members.size} payment log(s) and receipts inside it?",
                confirmLabel = stringResource(R.string.btn_delete_month),
                isBusy = isBusy,
                onConfirm = {
                    viewModel.deleteBillingMonth(
                        month = month,
                        members = members,
                        stats = stats,
                        onDone = { deletingMonthContext = null },
                    )
                },
                onDismiss = { deletingMonthContext = null },
            )
        }
    }

    if (showSpotifyHintDialog) {
        SpotifySetupHintDialog(onDismiss = { showSpotifyHintDialog = false })
    }

    if (needsSpotifyEmailPrompt) {
        SpotifyEmailOnboardingDialog(
            googleEmail = session.email,
            isBusy = isBusy,
            onSave = { spotifyEmail ->
                viewModel.submitInitialSpotifyEmail(spotifyEmail)
            },
            onSkip = { viewModel.dismissSpotifyEmailPromptForSession() },
        )
    }

    availableUpdate?.let { release ->
        AppUpdateAvailableDialog(
            release = release,
            onDownload = { viewModel.downloadAppUpdate(context, release) },
            onDismiss = { viewModel.dismissAvailableUpdate() },
        )
    }
}
