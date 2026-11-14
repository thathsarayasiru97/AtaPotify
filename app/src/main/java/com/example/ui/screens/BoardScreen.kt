package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.data.model.WhitelistMember
import com.example.data.model.calculateMonthlyBillingSummary
import com.example.data.repository.AccessResolution
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
    spotifyConnected: Boolean,
    spotifyName: String,
    isSpotifyClientConfigured: Boolean,
    isBusy: Boolean,
    onOpenStartMonth: () -> Unit,
    onOpenAddMemberToMonth: () -> Unit,
    onDeleteCurrentMonth: () -> Unit,
    onUploadSlip: (android.net.Uri, MemberPaymentStatus) -> Unit,
    onViewSlip: (MemberPaymentStatus) -> Unit,
    onConfirmPaid: (MemberPaymentStatus) -> Unit,
    onRewindPaid: (MemberPaymentStatus) -> Unit,
    onEditPayment: (MemberPaymentStatus) -> Unit,
    onDeletePayment: (MemberPaymentStatus) -> Unit,
    onRemindMember: (MemberPaymentStatus, String) -> Unit,
    onOpenOverdueQueue: () -> Unit,
    onConnectSpotify: () -> Unit,
    onSyncSpotify: () -> Unit,
    onShowSpotifySetupHint: () -> Unit,
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Payments, 1 = Leaderboard
    var pendingUploadTarget by remember { androidx.compose.runtime.mutableStateOf<MemberPaymentStatus?>(null) }

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

    val billingSummary = remember(members) {
        calculateMonthlyBillingSummary(members)
    }

    val leaderboardRoster = remember(stats, members, adminDisplayName, session) {
        buildLeaderboardRoster(
            syncedStats = stats,
            monthMembers = members,
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
                    isAdmin = session.isAdmin,
                    onStartNewMonth = onOpenStartMonth,
                )
            }

            if (currentMonth != null) {
                item {
                    MonthlyBillingSummaryCard(
                        month = currentMonth,
                        summary = billingSummary,
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
                            Text("Payments (${members.size})")
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
                item {
                    EmptyBoardState(
                        isAdmin = session.isAdmin,
                        onStartNewMonth = onOpenStartMonth,
                    )
                }
            } else if (selectedSection == 0) {
                if (session.isAdmin) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Payment Logs (${members.size})",
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

                items(members, key = { it.memberKey }) { member ->
                    val isSelf = member.memberEmail.equals(session.email, ignoreCase = true)
                    val phone = phoneByKey[member.memberKey].orEmpty()
                    MemberPaymentCard(
                        member = member,
                        isSelf = isSelf,
                        isAdmin = session.isAdmin,
                        whatsappDigits = phone,
                        isBusy = isBusy,
                        onUploadSlipClick = {
                            pendingUploadTarget = member
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onViewSlipClick = { onViewSlip(member) },
                        onMarkPaidClick = { onConfirmPaid(member) },
                        onRewindPaidClick = { onRewindPaid(member) },
                        onEditPaymentClick = { onEditPayment(member) },
                        onDeletePaymentClick = { onDeletePayment(member) },
                        onRemindClick = { onRemindMember(member, phone) },
                    )
                }
            } else {
                item {
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

                if (leaderboardRoster.isEmpty()) {
                    item {
                        EmptyLeaderboardState()
                    }
                } else {
                    items(leaderboardRoster.withIndex().toList(), key = { it.value.memberKey }) { (index, stat) ->
                        ListeningLeaderboardCard(
                            rank = index + 1,
                            stat = stat,
                            amountLkr = currentMonth.amountLkr,
                            isSelf = stat.memberEmail.equals(session.email, ignoreCase = true),
                            isAdminEntry = stat.memberEmail.equals(AtaPotifyConstants.ADMIN_EMAIL, ignoreCase = true),
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
    isAdmin: Boolean,
    onStartNewMonth: () -> Unit,
) {
    val amountLkr = month?.amountLkr ?: defaultAmountLkr

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
                        colors = listOf(Color(0xFF1A432A), SurfaceDark),
                        radius = 750f,
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
                            text = if (month != null) "Cycle ${month.monthId} • Fixed Split" else "Start a month to track payments",
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${summary.paidCount} of ${summary.totalCount} Paid",
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
                            progress = { summary.collectionFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = SpotifyEmerald,
                            trackColor = SurfaceHighest,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniCounterChip("Paid: ${summary.paidCount}", SpotifyEmerald, EmeraldContainer)
                            MiniCounterChip("Verifying: ${summary.verifyingCount}", AmberWarning, AmberContainer)
                            MiniCounterChip("Pending: ${summary.pendingCount}", CoralPending, CoralContainer)
                        }
                    }
                }

                if (isAdmin) {
                    Button(
                        onClick = onStartNewMonth,
                        modifier = Modifier
                            .fillMaxWidth()
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
            }
        }
    }
}

@Composable
private fun MonthlyBillingSummaryCard(
    month: PaymentMonth,
    summary: MonthlyBillingSummary,
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
                val paidWeight = summary.collectedLkr.toFloat() / summary.expectedTotalLkr.toFloat()
                val verifyingWeight = summary.verifyingLkr.toFloat() / summary.expectedTotalLkr.toFloat()
                val pendingWeight = summary.pendingOnlyLkr.toFloat() / summary.expectedTotalLkr.toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighest),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (paidWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(paidWeight)
                                .height(8.dp)
                                .background(SpotifyEmerald),
                        )
                    }
                    if (verifyingWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(verifyingWeight)
                                .height(8.dp)
                                .background(AmberWarning),
                        )
                    }
                    if (pendingWeight > 0f) {
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
    isBusy: Boolean,
    onUploadSlipClick: () -> Unit,
    onViewSlipClick: () -> Unit,
    onMarkPaidClick: () -> Unit,
    onRewindPaidClick: () -> Unit = {},
    onEditPaymentClick: () -> Unit = {},
    onDeletePaymentClick: () -> Unit = {},
    onRemindClick: () -> Unit,
) {
    val ageDays = member.pendingAgeDays()
    val isOverdue = member.status == AtaPotifyConstants.STATUS_PENDING && ageDays >= AtaPotifyConstants.OVERDUE_DAYS_THRESHOLD

    Card(
        modifier = Modifier
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

                StatusPill(status = member.status)
            }

            // Action rows for Self or Admin
            AnimatedVisibility(
                visible = (isSelf && member.status != AtaPotifyConstants.STATUS_PAID) || isAdmin || (isSelf && member.hasReceipt),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasPrimaryRow = (isSelf && member.status != AtaPotifyConstants.STATUS_PAID) ||
                        ((isSelf || isAdmin) && member.hasReceipt) ||
                        (isAdmin && member.status != AtaPotifyConstants.STATUS_PAID) ||
                        (isAdmin && member.status == AtaPotifyConstants.STATUS_PAID)

                    if (hasPrimaryRow) {
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

                            if (isAdmin && member.status == AtaPotifyConstants.STATUS_PAID) {
                                OutlinedButton(
                                    onClick = onRewindPaidClick,
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("rewind_paid_${member.memberKey}"),
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_rewind_paid), color = AmberWarning)
                                }
                            }
                        }
                    }

                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onEditPaymentClick,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_payment_${member.memberKey}"),
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.cd_edit_payment),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_edit_payment), color = MaterialTheme.colorScheme.onSurface)
                            }

                            OutlinedButton(
                                onClick = onDeletePaymentClick,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_payment_${member.memberKey}"),
                                border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_remove_payment),
                                    tint = CoralPending,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_remove_payment), color = CoralPending)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (label, fg, bg, icon) = when (status) {
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

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
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
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = stringResource(R.string.cd_spotify_stats),
                    tint = SpotifyEmerald,
                    modifier = Modifier.size(24.dp),
                )
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
    amountLkr: Long,
    isSelf: Boolean,
    isAdminEntry: Boolean = false,
) {
    val hours = stat.minutesListened / 60.0
    val costPerHour = if (hours >= 0.1) (amountLkr / hours).toInt() else null
    val hasSynced = stat.syncedAt != null || stat.minutesListened > 0L || stat.tracksCounted > 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    }
                }
                if (stat.topArtist.isNotBlank()) {
                    Text(
                        text = "Top artist: ${stat.topArtist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (!hasSynced) {
                    Text(
                        text = if (isSelf) "Tap Connect / Sync Spotify above" else "Waiting for Spotify sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                if (stat.topTrack.isNotBlank()) {
                    Text(
                        text = "On repeat: ${stat.topTrack}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stat.minutesListened} min",
                    style = MonoNumericLarge.copy(fontSize = 18.sp),
                    color = if (hasSynced) SpotifyEmerald else TextSecondary,
                )
                Text(
                    text = when {
                        costPerHour != null -> "LKR $costPerHour/hr"
                        hasSynced -> "${stat.tracksCounted} tracks"
                        else -> "Not synced"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
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
                    "Add your friends in the Admin tab, then tap Start New Month to generate the first board."
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
                text = "Connect Spotify above and tap Sync Stats to rank who is getting the best value out of their LKR share.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
