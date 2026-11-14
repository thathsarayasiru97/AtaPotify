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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.PaymentMonth
import com.example.data.model.WhitelistMember
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoardScreen(
    session: AccessResolution.Authorized,
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
    onUploadSlip: (android.net.Uri, MemberPaymentStatus) -> Unit,
    onViewSlip: (MemberPaymentStatus) -> Unit,
    onConfirmPaid: (MemberPaymentStatus) -> Unit,
    onRemindMember: (MemberPaymentStatus, String) -> Unit,
    onOpenOverdueQueue: () -> Unit,
    onConnectSpotify: () -> Unit,
    onSyncSpotify: () -> Unit,
    onShowSpotifySetupHint: () -> Unit,
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Payments, 1 = Listening Value
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
                    members = members,
                    defaultAmountLkr = defaultAmountLkr,
                    isAdmin = session.isAdmin,
                    onStartNewMonth = onOpenStartMonth,
                )
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
                            Text("Spotify Value")
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

                if (stats.isEmpty()) {
                    item {
                        EmptyLeaderboardState()
                    }
                } else {
                    items(stats.withIndex().toList(), key = { it.value.memberKey }) { (index, stat) ->
                        ListeningLeaderboardCard(
                            rank = index + 1,
                            stat = stat,
                            amountLkr = currentMonth.amountLkr,
                            isSelf = stat.memberEmail.equals(session.email, ignoreCase = true),
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
    members: List<MemberPaymentStatus>,
    defaultAmountLkr: Long,
    isAdmin: Boolean,
    onStartNewMonth: () -> Unit,
) {
    val paidCount = members.count { it.status == AtaPotifyConstants.STATUS_PAID }
    val verifyingCount = members.count { it.status == AtaPotifyConstants.STATUS_VERIFYING }
    val pendingCount = members.count { it.status == AtaPotifyConstants.STATUS_PENDING }
    val totalCount = members.size.coerceAtLeast(1)
    val progress = if (members.isEmpty()) 0f else paidCount.toFloat() / totalCount.toFloat()
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
                                text = "LKR $amountLkr",
                                style = MonoNumericLarge.copy(fontSize = 18.sp),
                                color = SpotifyEmerald,
                            )
                            Text(
                                text = "per friend",
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
                                text = "$paidCount of ${members.size} Paid",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "LKR ${paidCount * amountLkr} / ${members.size * amountLkr}",
                                style = MonoNumericSmall,
                                color = TextSecondary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = SpotifyEmerald,
                            trackColor = SurfaceHighest,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniCounterChip("Paid: $paidCount", SpotifyEmerald, EmeraldContainer)
                            MiniCounterChip("Verifying: $verifyingCount", AmberWarning, AmberContainer)
                            MiniCounterChip("Pending: $pendingCount", CoralPending, CoralContainer)
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

            // Action row for Self or Admin
            AnimatedVisibility(
                visible = (isSelf && member.status != AtaPotifyConstants.STATUS_PAID) || isAdmin || (isSelf && member.hasReceipt),
            ) {
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
                            "Tap Sync Stats to add your recent listening minutes to this month's leaderboard."
                        } else {
                            "See how much value you get out of your monthly LKR split."
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

@Composable
fun ListeningLeaderboardCard(
    rank: Int,
    stat: ListeningStat,
    amountLkr: Long,
    isSelf: Boolean,
) {
    val hours = stat.minutesListened / 60.0
    val costPerHour = if (hours >= 0.1) (amountLkr / hours).toInt() else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(
            1.dp,
            if (rank == 1) SpotifyEmerald.copy(alpha = 0.5f) else BorderSubtle,
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
                    .background(if (rank == 1) EmeraldContainer else SurfaceHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$rank",
                    style = MonoNumericSmall,
                    color = if (rank == 1) SpotifyEmerald else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.displayName + if (isSelf) " (You)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (stat.topArtist.isNotBlank()) {
                    Text(
                        text = "Top artist: ${stat.topArtist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    color = SpotifyEmerald,
                )
                Text(
                    text = if (costPerHour != null) "LKR $costPerHour/hr" else "${stat.tracksCounted} tracks",
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
