package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.MonthlyBillingSummary
import com.example.data.model.PaymentMonth
import com.example.data.model.PlanConfig
import com.example.data.model.WhitelistMember
import com.example.data.model.calculateMonthlyBillingSummary
import com.example.data.repository.AccessResolution
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.AnimatedStreakBadge
import com.example.ui.components.FamilyGroupBadge
import com.example.ui.components.FamilyGroupFilterBar
import com.example.ui.components.FamilyMixCard
import com.example.ui.components.PinnedAnnouncementBanner
import com.example.ui.components.pressBounceClickable
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSoftText
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.OverdueReminderCandidate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BoardScreen(
    session: AccessResolution.Authorized,
    adminDisplayName: String,
    currentMonth: PaymentMonth?,
    members: List<MemberPaymentStatus>,
    stats: List<ListeningStat>,
    whitelist: List<WhitelistMember>,
    overdueReminders: List<OverdueReminderCandidate>,
    defaultAmountLkr: Long,
    planConfig: PlanConfig = PlanConfig(),
    paymentStreaks: Map<String, Int> = emptyMap(),
    selectedGroupFilter: String = AtaPotifyConstants.GROUP_ALL,
    onSelectGroupFilter: (String) -> Unit = {},
    spotifyConnected: Boolean,
    spotifyName: String,
    isSpotifyClientConfigured: Boolean,
    isBusy: Boolean,
    onOpenStartMonth: () -> Unit,
    onOpenAddMemberToMonth: () -> Unit,
    onDeleteCurrentMonth: () -> Unit,
    onUploadSlip: (android.net.Uri, MemberPaymentStatus) -> Unit,
    onViewSlip: (MemberPaymentStatus) -> Unit,
    onOpenVerifiedReceipt: (MemberPaymentStatus) -> Unit = {},
    onConfirmPaid: (MemberPaymentStatus) -> Unit,
    onAdvancePayClick: (MemberPaymentStatus) -> Unit = {},
    onRewindPaid: (MemberPaymentStatus) -> Unit,
    onEditPayment: (MemberPaymentStatus) -> Unit,
    onDeletePayment: (MemberPaymentStatus) -> Unit,
    onRemindMember: (MemberPaymentStatus, String) -> Unit,
    onCopyText: (String, String) -> Unit = { _, _ -> },
    onRemoveAnnouncementSlide: (Int) -> Unit = {},
    onClearAnnouncement: () -> Unit = {},
    onShareMonthlySummary: () -> Unit = {},
    onOpenOverdueQueue: () -> Unit,
    onConnectSpotify: () -> Unit,
    onSyncSpotify: () -> Unit,
    onShowSpotifySetupHint: () -> Unit,
    onOpenSpotifyQuery: (String) -> Unit = {},
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Payments, 1 = Leaderboard
    var pendingUploadTarget by remember { mutableStateOf<MemberPaymentStatus?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            val target = pendingUploadTarget
            pendingUploadTarget = null
            if (uri != null && target != null) {
                onUploadSlip(uri, target)
            }
        },
    )

    val phoneByKey = remember(whitelist) {
        whitelist.associate { it.memberKey to it.phone }
    }

    val groupByKey = remember(whitelist, members) {
        val map = mutableMapOf<String, String>()
        members.forEach { map[it.memberKey] = it.normalizedGroup }
        whitelist.forEach { wl ->
            if (wl.familyGroup == AtaPotifyConstants.GROUP_2 || map[wl.memberKey] == null) {
                map[wl.memberKey] = wl.normalizedGroup
            }
        }
        map
    }

    val group1Count = remember(members, groupByKey) {
        members.count { (groupByKey[it.memberKey] ?: it.normalizedGroup) == AtaPotifyConstants.GROUP_1 }
    }
    val group2Count = remember(members, groupByKey) {
        members.count { (groupByKey[it.memberKey] ?: it.normalizedGroup) == AtaPotifyConstants.GROUP_2 }
    }

    val filteredMembers = remember(members, selectedGroupFilter, groupByKey) {
        when (selectedGroupFilter) {
            AtaPotifyConstants.GROUP_1 -> members.filter {
                (groupByKey[it.memberKey] ?: it.normalizedGroup) == AtaPotifyConstants.GROUP_1
            }
            AtaPotifyConstants.GROUP_2 -> members.filter {
                (groupByKey[it.memberKey] ?: it.normalizedGroup) == AtaPotifyConstants.GROUP_2
            }
            else -> members
        }
    }

    val filteredStats = remember(stats, filteredMembers, selectedGroupFilter, groupByKey) {
        if (selectedGroupFilter == AtaPotifyConstants.GROUP_ALL) {
            stats
        } else {
            val allowedKeys = filteredMembers.map { it.memberKey }.toSet()
            val adminKey = AtaPotifyConstants.memberKeyForEmail(AtaPotifyConstants.ADMIN_EMAIL)
            stats.filter { stat ->
                stat.memberKey in allowedKeys ||
                    stat.memberKey == adminKey ||
                    groupByKey[stat.memberKey] == selectedGroupFilter
            }
        }
    }

    val billingSummary = remember(filteredMembers) {
        calculateMonthlyBillingSummary(filteredMembers)
    }

    val leaderboardRoster = remember(filteredStats, filteredMembers, adminDisplayName, session) {
        buildLeaderboardRoster(
            syncedStats = filteredStats,
            monthMembers = filteredMembers,
            adminDisplayName = adminDisplayName,
            session = session,
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MonthHeroCard(
                    month = currentMonth,
                    summary = billingSummary,
                    defaultAmountLkr = defaultAmountLkr,
                    selectedGroupFilter = selectedGroupFilter,
                    isAdmin = session.isAdmin,
                    onStartNewMonth = onOpenStartMonth,
                    onShareMonthlySummary = onShareMonthlySummary,
                )
            }

            if (planConfig.hasPinnedAnnouncement) {
                item(key = "pinned_announcement_banner") {
                    PinnedAnnouncementBanner(
                        announcements = planConfig.effectiveAnnouncements,
                        isAdmin = session.isAdmin,
                        onRemoveSlide = onRemoveAnnouncementSlide,
                        onClearAnnouncement = onClearAnnouncement,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (session.isAdmin && overdueReminders.isNotEmpty()) {
                item {
                    OverdueAutoReminderBanner(
                        overdueCount = overdueReminders.size,
                        onOpenQueue = onOpenOverdueQueue,
                    )
                }
            }

            if (currentMonth != null) {
                item(key = "board_family_group_filter") {
                    FamilyGroupFilterBar(
                        selectedGroup = selectedGroupFilter,
                        allCount = members.size,
                        group1Count = group1Count,
                        group2Count = group2Count,
                        onSelectGroup = onSelectGroupFilter,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (currentMonth != null) {
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SegmentedButton(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("board_tab_payments"),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldContainer,
                                activeContentColor = SpotifyEmerald,
                                inactiveContainerColor = SurfaceDark,
                                inactiveContentColor = TextSecondary,
                            ),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        ) {
                            Text("Payments (${filteredMembers.size})")
                        }
                        SegmentedButton(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("board_tab_leaderboard"),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldContainer,
                                activeContentColor = SpotifyEmerald,
                                inactiveContainerColor = SurfaceDark,
                                inactiveContentColor = TextSecondary,
                            ),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        ) {
                            Text(stringResource(R.string.tab_leaderboard))
                        }
                    }
                }
            }

            if (currentMonth == null) {
                item(key = "empty_board") {
                    EmptyBoardState(
                        isAdmin = session.isAdmin,
                        onStartNewMonth = onOpenStartMonth,
                    )
                }
            } else if (selectedSection == 0) {
                if (planConfig.hasBankDetails) {
                    item(key = "bank_details_card") {
                        Box(modifier = Modifier.animateItem()) {
                            BankTransferDetailsCard(
                                planConfig = planConfig,
                                onCopyText = onCopyText,
                            )
                        }
                    }
                }

                if (session.isAdmin) {
                    item(key = "payment_logs_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Payment Logs (${filteredMembers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onOpenAddMemberToMonth,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("add_member_to_month_button"),
                                    border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = SpotifyEmerald,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_add_to_month), color = SpotifyEmerald)
                                }
                                OutlinedButton(
                                    onClick = onDeleteCurrentMonth,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_current_month_button"),
                                    border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.btn_delete_month),
                                        tint = CoralPending,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredMembers, key = { it.memberKey }) { member ->
                    val isSelf = member.memberEmail.equals(session.email, ignoreCase = true)
                    val phone = phoneByKey[member.memberKey].orEmpty()
                    val streak = paymentStreaks[member.memberKey] ?: 0
                    val resolvedGroup = groupByKey[member.memberKey] ?: member.normalizedGroup
                    MemberPaymentCard(
                        member = member,
                        isSelf = isSelf,
                        isAdmin = session.isAdmin,
                        whatsappDigits = phone,
                        streakCount = streak,
                        familyGroup = resolvedGroup,
                        isBusy = isBusy,
                        modifier = Modifier.animateItem(),
                        onUploadSlipClick = {
                            pendingUploadTarget = member
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onViewSlipClick = { onViewSlip(member) },
                        onOpenVerifiedReceiptClick = { onOpenVerifiedReceipt(member) },
                        onMarkPaidClick = { onConfirmPaid(member) },
                        onAdvancePayClick = { onAdvancePayClick(member) },
                        onRewindPaidClick = { onRewindPaid(member) },
                        onEditPaymentClick = { onEditPayment(member) },
                        onDeletePaymentClick = { onDeletePayment(member) },
                        onRemindClick = { onRemindMember(member, phone) },
                    )
                }
            } else {
                item(key = "spotify_connect_card") {
                    Box(modifier = Modifier.animateItem()) {
                        SpotifyConnectCard(
                            isConfigured = isSpotifyClientConfigured,
                            isConnected = spotifyConnected,
                            spotifyName = spotifyName,
                            isBusy = isBusy,
                            onConnect = {
                                if (isSpotifyClientConfigured) onConnectSpotify()
                                else onShowSpotifySetupHint()
                            },
                            onSync = onSyncSpotify,
                        )
                    }
                }

                if (leaderboardRoster.any { it.topTrack.isNotBlank() || it.topArtist.isNotBlank() }) {
                    item(key = "family_mix_card") {
                        FamilyMixCard(
                            stats = leaderboardRoster,
                            onOpenInSpotify = onOpenSpotifyQuery,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (leaderboardRoster.isEmpty()) {
                    item(key = "empty_leaderboard") {
                        Box(modifier = Modifier.animateItem()) {
                            EmptyLeaderboardState()
                        }
                    }
                } else {
                    val topMinutes = leaderboardRoster.maxOfOrNull { it.minutesListened }?.coerceAtLeast(1L) ?: 1L
                    items(leaderboardRoster.withIndex().toList(), key = { "lb_${it.value.memberKey}" }) { (index, stat) ->
                        val isAdminRow = stat.memberEmail.equals(AtaPotifyConstants.ADMIN_EMAIL, ignoreCase = true)
                        ListeningLeaderboardCard(
                            rank = index + 1,
                            stat = stat,
                            maxMinutes = topMinutes,
                            isSelf = stat.memberEmail.equals(session.email, ignoreCase = true),
                            isAdminEntry = isAdminRow,
                            familyGroup = if (isAdminRow) null else groupByKey[stat.memberKey],
                            onOpenInSpotify = onOpenSpotifyQuery,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MonthHeroCard(
    month: PaymentMonth?,
    summary: MonthlyBillingSummary,
    defaultAmountLkr: Long,
    selectedGroupFilter: String = AtaPotifyConstants.GROUP_ALL,
    isAdmin: Boolean,
    onStartNewMonth: () -> Unit,
    onShareMonthlySummary: () -> Unit,
) {
    val amountLkr = month?.amountLkr ?: defaultAmountLkr
    val groupSubtitle = if (selectedGroupFilter == AtaPotifyConstants.GROUP_1 || selectedGroupFilter == AtaPotifyConstants.GROUP_2) {
        " • ${AtaPotifyConstants.familyGroupLabel(selectedGroupFilter)}"
    } else {
        " • All Family Groups"
    }
    val infiniteTransition = rememberInfiniteTransition(label = "hero_aurora_anim")
    val auroraShift by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero_aurora_shift",
    )
    val auroraRadius by infiniteTransition.animateFloat(
        initialValue = 680f,
        targetValue = 920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero_aurora_radius",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E5233), Color(0xFF143521), SurfaceDark),
                        center = Offset(auroraShift, 120f),
                        radius = auroraRadius,
                    )
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = month?.displayLabel ?: "No Active Billing Cycle",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (month != null) "Cycle ${month.monthId}$groupSubtitle" else "Start a month to track payments",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = "LKR ${formatLkr(amountLkr)}",
                                style = MonoNumericLarge.copy(fontSize = 18.sp),
                                color = SpotifyEmerald,
                            )
                            Text(
                                text = "default share",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoftText,
                            )
                        }
                    }
                }

                if (month != null) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = summary.collectionFraction.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label = "hero_collection_progress",
                    )
                    val animatedPaid by animateIntAsState(
                        targetValue = summary.paidCount,
                        animationSpec = tween(durationMillis = 450),
                        label = "hero_paid_count",
                    )
                    val animatedVerifying by animateIntAsState(
                        targetValue = summary.verifyingCount,
                        animationSpec = tween(durationMillis = 450),
                        label = "hero_verifying_count",
                    )
                    val animatedPending by animateIntAsState(
                        targetValue = summary.pendingCount,
                        animationSpec = tween(durationMillis = 450),
                        label = "hero_pending_count",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "$animatedPaid of ${summary.totalCount} Paid",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "LKR ${formatLkr(summary.collectedLkr)} / ${formatLkr(summary.expectedTotalLkr)}",
                                style = MonoNumericSmall,
                                color = TextSecondary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = SpotifyEmerald,
                            trackColor = SurfaceHighest,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniCounterChip("Paid: $animatedPaid", SpotifyEmerald, EmeraldContainer)
                            MiniCounterChip("Verifying: $animatedVerifying", AmberWarning, AmberContainer)
                            MiniCounterChip("Pending: $animatedPending", CoralPending, CoralContainer)
                        }
                    }
                }

                if (isAdmin || month != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isAdmin) {
                            Button(
                                onClick = onStartNewMonth,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("start_new_month_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotifyEmerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_start_new_month),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        if (month != null) {
                            OutlinedButton(
                                onClick = onShareMonthlySummary,
                                modifier = Modifier
                                    .then(if (isAdmin) Modifier else Modifier.fillMaxWidth())
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_monthly_summary_button"),
                                border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share_summary),
                                    tint = SpotifyEmerald,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.btn_share_summary),
                                    color = SpotifyEmerald,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankTransferDetailsCard(
    planConfig: PlanConfig,
    onCopyText: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_transfer_details_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = SpotifyEmerald,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.title_bank_details),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val bankLine = listOf(planConfig.bankName, planConfig.bankBranch)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        if (bankLine.isNotBlank()) {
                            Text(
                                text = bankLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                if (planConfig.bankAccountNumber.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { onCopyText("Account Number", planConfig.bankAccountNumber) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_bank_account_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldContainer,
                            contentColor = SpotifyEmerald,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_text),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Acc No.")
                    }
                }
            }

            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (planConfig.bankAccountNumber.isNotBlank()) {
                            Text(
                                text = planConfig.bankAccountNumber,
                                style = MonoNumericLarge.copy(fontSize = 16.sp),
                                color = SpotifyEmerald,
                            )
                        }
                        if (planConfig.bankAccountHolder.isNotBlank()) {
                            Text(
                                text = "Holder: ${planConfig.bankAccountHolder}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    TextButton(
                        onClick = { onCopyText("Bank Details", planConfig.formattedBankCopyText()) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_all_bank_details_button"),
                    ) {
                        Text("Copy All", color = SpotifyEmerald)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MonthlyBillingSummaryCard(
    month: PaymentMonth,
    members: List<MemberPaymentStatus>,
    summary: MonthlyBillingSummary,
    selfEmail: String,
    isAdmin: Boolean,
    isBusy: Boolean,
    onQuickStatusOverride: (MemberPaymentStatus, String) -> Unit,
) {
    val isSettled = summary.totalCount > 0 && summary.netBalanceLkr >= 0L
    val pctSettled = (summary.collectionFraction * 100f).roundToInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("billing_summary_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isSettled) SpotifyEmerald.copy(alpha = 0.45f) else BorderSubtle,
        ),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.cd_billing_summary),
                            tint = SpotifyEmerald,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.title_monthly_billing_summary),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${month.displayLabel} • Target LKR ${formatLkr(summary.expectedTotalLkr)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }

                Surface(
                    color = if (isSettled) EmeraldContainer else AmberContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (isSettled) "100% Settled" else "$pctSettled% Collected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSettled) SpotifyEmerald else AmberWarning,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            if (summary.expectedTotalLkr > 0L) {
                val paidWeight by animateFloatAsState(
                    targetValue = (summary.collectedLkr.toFloat() / summary.expectedTotalLkr.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "summary_paid_weight",
                )
                val verifyingWeight by animateFloatAsState(
                    targetValue = (summary.verifyingLkr.toFloat() / summary.expectedTotalLkr.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "summary_verifying_weight",
                )
                val pendingWeight by animateFloatAsState(
                    targetValue = (summary.pendingOnlyLkr.toFloat() / summary.expectedTotalLkr.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "summary_pending_weight",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighest),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (paidWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(paidWeight)
                                .height(8.dp)
                                .background(SpotifyEmerald),
                        )
                    }
                    if (verifyingWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(verifyingWeight)
                                .height(8.dp)
                                .background(AmberWarning),
                        )
                    }
                    if (pendingWeight > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(pendingWeight)
                                .height(8.dp)
                                .background(CoralPending),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryMetricTile(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("summary_collected_amount"),
                    label = stringResource(R.string.label_total_collected),
                    value = "LKR ${formatLkr(summary.collectedLkr)}",
                    subtext = "${summary.paidCount} of ${summary.totalCount} paid",
                    accentColor = SpotifyEmerald,
                    containerColor = SurfaceElevated,
                )

                val pendingSubtext = buildString {
                    append("${summary.pendingCount} pending")
                    if (summary.verifyingCount > 0) {
                        append(" • ${summary.verifyingCount} verifying")
                    }
                }
                SummaryMetricTile(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("summary_pending_amount"),
                    label = stringResource(R.string.label_pending_amounts),
                    value = "LKR ${formatLkr(summary.totalPendingLkr)}",
                    subtext = pendingSubtext,
                    accentColor = if (summary.totalPendingLkr == 0L) SpotifyEmerald else AmberWarning,
                    containerColor = SurfaceElevated,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_net_balance"),
                color = if (isSettled) EmeraldContainer.copy(alpha = 0.55f) else CoralContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSettled) SpotifyEmerald.copy(alpha = 0.4f) else CoralPending.copy(alpha = 0.35f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_net_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = when {
                                summary.totalCount == 0 -> "No payment logs recorded yet"
                                summary.netBalanceLkr == 0L -> "All scheduled shares collected"
                                else -> "Remaining shortfall vs LKR ${formatLkr(summary.expectedTotalLkr)} expected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Text(
                        text = when {
                            summary.netBalanceLkr < 0L -> "-LKR ${formatLkr(abs(summary.netBalanceLkr))}"
                            summary.netBalanceLkr > 0L -> "+LKR ${formatLkr(summary.netBalanceLkr)}"
                            else -> "LKR 0"
                        },
                        style = MonoNumericLarge.copy(fontSize = 18.sp),
                        color = if (isSettled || summary.totalCount == 0) SpotifyEmerald else CoralPending,
                    )
                }
            }

            if (members.isNotEmpty()) {
                HorizontalDivider(color = BorderSubtle)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.title_member_status_breakdown),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isAdmin) {
                                stringResource(R.string.hint_tap_status_override)
                            } else {
                                "${members.size} members"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAdmin) SpotifyEmerald else TextMuted,
                        )
                    }

                    members.forEach { member ->
                        BillingSummaryMemberRow(
                            member = member,
                            isSelf = member.memberEmail.equals(selfEmail, ignoreCase = true),
                            isAdmin = isAdmin,
                            isBusy = isBusy,
                            onQuickStatusOverride = { targetStatus ->
                                onQuickStatusOverride(member, targetStatus)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingSummaryMemberRow(
    member: MemberPaymentStatus,
    isSelf: Boolean,
    isAdmin: Boolean,
    isBusy: Boolean,
    onQuickStatusOverride: (String) -> Unit,
) {
    val isPaid = member.status == AtaPotifyConstants.STATUS_PAID
    val isVerifying = member.status == AtaPotifyConstants.STATUS_VERIFYING
    val isPending = !isPaid && !isVerifying

    val indicatorColor = when {
        isPaid -> SpotifyEmerald
        isVerifying -> AmberWarning
        else -> CoralPending
    }
    val indicatorContainer = when {
        isPaid -> EmeraldContainer
        isVerifying -> AmberContainer
        else -> CoralContainer
    }
    val indicatorIcon = when {
        isPaid -> Icons.Default.CheckCircle
        isVerifying -> Icons.Default.HourglassTop
        else -> Icons.Default.Schedule
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_member_row_${member.memberKey}"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(indicatorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = indicatorIcon,
                    contentDescription = member.status,
                    tint = indicatorColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) "${member.displayName} (You)" else member.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("LKR ${formatLkr(member.amountLkr)}")
                        when {
                            isVerifying -> append(" • Verifying slip")
                            isPaid -> append(" • Settled")
                            else -> append(" • Unpaid")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isVerifying) AmberWarning else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isAdmin) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickStatusOverrideChip(
                        label = stringResource(R.string.status_paid),
                        selected = isPaid,
                        activeColor = SpotifyEmerald,
                        activeContainer = EmeraldContainer,
                        enabled = !isBusy && !isPaid,
                        contentDescription = stringResource(R.string.cd_quick_mark_paid, member.displayName),
                        testTag = "summary_override_paid_${member.memberKey}",
                        onClick = { onQuickStatusOverride(AtaPotifyConstants.STATUS_PAID) },
                    )
                    QuickStatusOverrideChip(
                        label = stringResource(R.string.status_pending),
                        selected = isPending,
                        activeColor = CoralPending,
                        activeContainer = CoralContainer,
                        enabled = !isBusy && !isPending,
                        contentDescription = stringResource(R.string.cd_quick_mark_pending, member.displayName),
                        testTag = "summary_override_pending_${member.memberKey}",
                        onClick = { onQuickStatusOverride(AtaPotifyConstants.STATUS_PENDING) },
                    )
                }
            } else {
                StatusPill(status = member.status)
            }
        }
    }
}

@Composable
private fun QuickStatusOverrideChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeContainer: Color,
    enabled: Boolean,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (selected) activeContainer else SurfaceDark,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) activeColor.copy(alpha = 0.7f) else BorderSubtle,
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SummaryMetricTile(
    modifier: Modifier,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    containerColor: Color,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MonoNumericLarge.copy(fontSize = 17.sp),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatLkr(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.US).format(amount)

@Composable
private fun MiniCounterChip(label: String, fg: Color, bg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun OverdueAutoReminderBanner(
    overdueCount: Int,
    onOpenQueue: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overdue_auto_reminder_banner"),
        colors = CardDefaults.cardColors(containerColor = AmberContainer),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = stringResource(R.string.cd_overdue_alert),
                tint = AmberWarning,
                modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$overdueCount friend(s) pending 7+ days",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberWarning,
                )
                Text(
                    text = "Automated check detected overdue payments ready for WhatsApp reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(
                onClick = onOpenQueue,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_overdue_queue_button"),
            ) {
                Text("Send", color = AmberWarning, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MemberPaymentCard(
    member: MemberPaymentStatus,
    isSelf: Boolean,
    isAdmin: Boolean,
    whatsappDigits: String,
    streakCount: Int = 0,
    familyGroup: String = member.normalizedGroup,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onUploadSlipClick: () -> Unit,
    onViewSlipClick: () -> Unit,
    onOpenVerifiedReceiptClick: () -> Unit = {},
    onMarkPaidClick: () -> Unit,
    onAdvancePayClick: () -> Unit = {},
    onRewindPaidClick: () -> Unit = {},
    onEditPaymentClick: () -> Unit = {},
    onDeletePaymentClick: () -> Unit = {},
    onRemindClick: () -> Unit,
) {
    val ageDays = member.pendingAgeDays()
    val isOverdue = member.status == AtaPotifyConstants.STATUS_PENDING && ageDays >= AtaPotifyConstants.OVERDUE_DAYS_THRESHOLD
    var showAdminMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_card_${member.memberKey}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelf) SurfaceElevated else SurfaceDark,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelf) SpotifyEmerald.copy(alpha = 0.45f) else BorderSubtle,
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (member.status == AtaPotifyConstants.STATUS_PAID) EmeraldContainer else SurfaceHighest
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = member.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (member.status == AtaPotifyConstants.STATUS_PAID) SpotifyEmerald else MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isSelf) {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        FamilyGroupBadge(familyGroup = familyGroup)
                        if (streakCount >= 1) {
                            AnimatedStreakBadge(streakCount = streakCount)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "LKR ${member.amountLkr}",
                            style = MonoNumericSmall,
                            color = TextSecondary,
                        )
                        if (isOverdue) {
                            Text(
                                text = "• ${ageDays}d overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                            )
                        } else if (member.status == AtaPotifyConstants.STATUS_PAID && member.confirmedAt != null) {
                            val dateStr = remember(member.confirmedAt) {
                                SimpleDateFormat("MMM d", Locale.US).format(member.confirmedAt.toDate())
                            }
                            Text(
                                text = "• Confirmed $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoftText,
                            )
                        }
                    }

                    if (isAdmin && whatsappDigits.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "+$whatsappDigits",
                                style = MonoNumericSmall.copy(fontSize = 11.sp),
                                color = TextMuted,
                            )
                        }
                    }
                }

                StatusPill(
                    status = member.status,
                    onClick = if (member.status == AtaPotifyConstants.STATUS_PAID) onOpenVerifiedReceiptClick else null,
                )

                if (isAdmin) {
                    Box {
                        IconButton(
                            onClick = { showAdminMenu = true },
                            enabled = !isBusy,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("member_overflow_${member.memberKey}"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_admin_actions),
                                tint = TextSecondary,
                            )
                        }

                        DropdownMenu(
                            expanded = showAdminMenu,
                            onDismissRequest = { showAdminMenu = false },
                            containerColor = SurfaceElevated,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Advance Pay (Multi-Month)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = SpotifyEmerald,
                                    )
                                },
                                onClick = {
                                    showAdminMenu = false
                                    onAdvancePayClick()
                                },
                                modifier = Modifier.testTag("advance_pay_${member.memberKey}"),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.btn_edit_payment)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                    )
                                },
                                onClick = {
                                    showAdminMenu = false
                                    onEditPaymentClick()
                                },
                                modifier = Modifier.testTag("edit_payment_${member.memberKey}"),
                            )
                            if (member.status == AtaPotifyConstants.STATUS_PAID) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.btn_rewind_paid), color = AmberWarning) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                        )
                                    },
                                    onClick = {
                                        showAdminMenu = false
                                        onRewindPaidClick()
                                    },
                                    modifier = Modifier.testTag("rewind_paid_${member.memberKey}"),
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.btn_remove_payment), color = CoralPending) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = CoralPending,
                                    )
                                },
                                onClick = {
                                    showAdminMenu = false
                                    onDeletePaymentClick()
                                },
                                modifier = Modifier.testTag("delete_payment_${member.memberKey}"),
                            )
                        }
                    }
                }
            }

            // Primary action row for Self, Paid Receipt, or Admin
            val hasPrimaryRow = (isSelf && member.status != AtaPotifyConstants.STATUS_PAID) ||
                ((isSelf || isAdmin) && member.hasReceipt) ||
                (isAdmin && member.status != AtaPotifyConstants.STATUS_PAID) ||
                ((isSelf || isAdmin) && member.status == AtaPotifyConstants.STATUS_PAID)

            AnimatedVisibility(visible = hasPrimaryRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isSelf && member.status == AtaPotifyConstants.STATUS_PENDING) {
                        Button(
                            onClick = onUploadSlipClick,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("upload_slip_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_upload_slip))
                        }
                    }

                    if (isSelf && member.status == AtaPotifyConstants.STATUS_VERIFYING) {
                        OutlinedButton(
                            onClick = onUploadSlipClick,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("replace_slip_button"),
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.btn_replace_slip))
                        }
                    }

                    if ((isSelf || isAdmin) && member.hasReceipt) {
                        FilledTonalButton(
                            onClick = onViewSlipClick,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("view_slip_${member.memberKey}"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isAdmin && member.status == AtaPotifyConstants.STATUS_VERIFYING) {
                                    AmberContainer
                                } else {
                                    SurfaceHighest
                                },
                                contentColor = if (isAdmin && member.status == AtaPotifyConstants.STATUS_VERIFYING) {
                                    AmberWarning
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isAdmin && member.status == AtaPotifyConstants.STATUS_VERIFYING) {
                                    stringResource(R.string.btn_review_slip)
                                } else {
                                    stringResource(R.string.btn_view_slip)
                                }
                            )
                        }
                    }

                    if ((isSelf || isAdmin) && member.status == AtaPotifyConstants.STATUS_PAID) {
                        FilledTonalButton(
                            onClick = onOpenVerifiedReceiptClick,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("verified_receipt_${member.memberKey}"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldContainer,
                                contentColor = SpotifyEmerald,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Verified Receipt")
                        }
                    }

                    if (isAdmin && member.status == AtaPotifyConstants.STATUS_PENDING) {
                        OutlinedButton(
                            onClick = onRemindClick,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("remind_${member.memberKey}"),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_remind), color = AmberWarning)
                        }
                    }

                    if (isAdmin && member.status != AtaPotifyConstants.STATUS_PAID) {
                        FilledTonalButton(
                            onClick = onMarkPaidClick,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("mark_paid_${member.memberKey}"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldContainer,
                                contentColor = SpotifyEmerald,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_mark_paid))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    status: String,
    onClick: (() -> Unit)? = null,
) {
    val (label, targetFg, targetBg, icon) = when (status) {
        AtaPotifyConstants.STATUS_PAID -> Quad(
            stringResource(R.string.status_paid),
            SpotifyEmerald,
            EmeraldContainer,
            Icons.Default.CheckCircle,
        )
        AtaPotifyConstants.STATUS_VERIFYING -> Quad(
            stringResource(R.string.status_verifying),
            AmberWarning,
            AmberContainer,
            Icons.Default.HourglassTop,
        )
        else -> Quad(
            stringResource(R.string.status_pending),
            CoralPending,
            CoralContainer,
            Icons.Default.Schedule,
        )
    }

    val fg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = tween(durationMillis = 320),
        label = "pill_fg",
    )
    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 320),
        label = "pill_bg",
    )
    val bounceScale by animateFloatAsState(
        targetValue = if (status == AtaPotifyConstants.STATUS_PAID) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pill_scale",
    )

    val pillModifier = if (onClick != null) {
        Modifier
            .scale(bounceScale)
            .pressBounceClickable(onClick = onClick)
    } else {
        Modifier.scale(bounceScale)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = pillModifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun SpotifyConnectCard(
    isConfigured: Boolean,
    isConnected: Boolean,
    spotifyName: String,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onSync: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isConnected) {
                        AnimatedEqualizerBars(isActive = true)
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = stringResource(R.string.cd_spotify_stats),
                            tint = SpotifyEmerald,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) {
                            "Spotify Connected${if (spotifyName.isNotBlank()) " ($spotifyName)" else ""}"
                        } else {
                            "Connect Your Spotify Account"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (!isConfigured) {
                            "Add SPOTIFY_CLIENT_ID in AI Studio Secrets to enable Spotify login."
                        } else if (isConnected) {
                            "Auto-syncs hourly in background & on open (accumulates plays + 4-week Top Artist/Track). Tap Sync Stats anytime."
                        } else {
                            "Connect Spotify for hourly background sync, listening minutes, and Leaderboard ranking."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Button(
                onClick = if (isConnected) onSync else onConnect,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("spotify_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Refresh else Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isConnected) stringResource(R.string.btn_sync_spotify)
                    else stringResource(R.string.btn_connect_spotify)
                )
            }
        }
    }
}

internal fun buildLeaderboardRoster(
    syncedStats: List<ListeningStat>,
    monthMembers: List<MemberPaymentStatus>,
    adminDisplayName: String,
    session: AccessResolution.Authorized,
): List<ListeningStat> {
    val byKey = syncedStats.associateBy { it.memberKey }.toMutableMap()
    val adminKey = AtaPotifyConstants.memberKeyForEmail(AtaPotifyConstants.ADMIN_EMAIL)
    if (!byKey.containsKey(adminKey)) {
        val resolvedAdminName = if (session.isAdmin && session.displayName.isNotBlank()) {
            session.displayName
        } else {
            adminDisplayName.ifBlank { "Admin" }
        }
        byKey[adminKey] = ListeningStat(
            memberKey = adminKey,
            memberEmail = AtaPotifyConstants.ADMIN_EMAIL,
            displayName = resolvedAdminName,
            minutesListened = 0L,
            tracksCounted = 0L,
        )
    }
    for (member in monthMembers) {
        if (!byKey.containsKey(member.memberKey)) {
            byKey[member.memberKey] = ListeningStat(
                memberKey = member.memberKey,
                memberEmail = member.memberEmail,
                displayName = member.displayName,
                minutesListened = 0L,
                tracksCounted = 0L,
            )
        }
    }
    return byKey.values.sortedWith(
        compareByDescending<ListeningStat> { it.minutesListened }
            .thenByDescending { it.tracksCounted }
            .thenBy { it.displayName.lowercase() }
    )
}

@Composable
fun ListeningLeaderboardCard(
    rank: Int,
    stat: ListeningStat,
    maxMinutes: Long = 1L,
    isSelf: Boolean,
    isAdminEntry: Boolean = false,
    familyGroup: String? = null,
    onOpenInSpotify: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasSynced = stat.syncedAt != null || stat.minutesListened > 0L || stat.tracksCounted > 0L
    val animatedMinutes by animateIntAsState(
        targetValue = stat.minutesListened.toInt(),
        animationSpec = tween(durationMillis = 550),
        label = "lb_minutes_${stat.memberKey}",
    )
    val targetFraction = if (hasSynced && maxMinutes > 0L) {
        (stat.minutesListened.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
    } else {
        0f
    }
    val animatedBarFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "lb_bar_${stat.memberKey}",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isSelf) SurfaceElevated else SurfaceDark),
        border = BorderStroke(
            1.dp,
            when {
                rank == 1 && hasSynced -> SpotifyEmerald.copy(alpha = 0.55f)
                isSelf -> SpotifyEmerald.copy(alpha = 0.35f)
                else -> BorderSubtle
            },
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (rank == 1 && hasSynced) EmeraldContainer else SurfaceHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "#$rank",
                        style = MonoNumericSmall,
                        color = if (rank == 1 && hasSynced) SpotifyEmerald else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stat.displayName + if (isSelf) " (You)" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isAdminEntry) {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        } else if (familyGroup != null) {
                            FamilyGroupBadge(familyGroup = familyGroup)
                        }
                        if (hasSynced) {
                            AnimatedEqualizerBars(
                                isActive = true,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                    if (stat.topArtist.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenInSpotify(stat.topArtist) }
                                .padding(vertical = 1.dp),
                        ) {
                            Text(
                                text = "Top artist: ${stat.topArtist}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open ${stat.topArtist} in Spotify",
                                tint = SpotifyEmerald.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    } else if (!hasSynced) {
                        Text(
                            text = if (isSelf) "Tap Connect / Sync Spotify above" else "Waiting for Spotify sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    if (stat.topTrack.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val query = listOf(stat.topTrack, stat.topArtist)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ")
                                    onOpenInSpotify(query)
                                }
                                .padding(vertical = 1.dp),
                        ) {
                            Text(
                                text = "On repeat: ${stat.topTrack}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoftText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open ${stat.topTrack} in Spotify",
                                tint = SpotifyEmerald.copy(alpha = 0.8f),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$animatedMinutes min",
                        style = MonoNumericLarge.copy(fontSize = 18.sp),
                        color = if (hasSynced) SpotifyEmerald else TextSecondary,
                    )
                    Text(
                        text = if (hasSynced) "${stat.tracksCounted} tracks" else "Not synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            if (hasSynced) {
                LinearProgressIndicator(
                    progress = { animatedBarFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = if (rank == 1) SpotifyEmerald else SpotifyEmerald.copy(alpha = 0.65f),
                    trackColor = SurfaceHighest,
                )
            }
        }
    }
}

@Composable
private fun EmptyBoardState(
    isAdmin: Boolean,
    onStartNewMonth: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "No billing cycle started yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isAdmin) {
                    "Approve or add friends in the Admin tab, then tap Start New Month to generate the first board."
                } else {
                    "Waiting for the plan admin to start this month's Spotify Family billing cycle."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (isAdmin) {
                Button(
                    onClick = onStartNewMonth,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.btn_start_new_month))
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboardState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = "No listening stats synced yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Connect Spotify above and tap Sync Stats to track who listens the most this month.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
