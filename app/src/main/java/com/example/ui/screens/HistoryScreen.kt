package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.AllTimeWrappedSummary
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.WhitelistMember
import com.example.data.model.calculateMonthlyBillingSummary
import com.example.data.repository.AccessResolution
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSoftText
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    session: AccessResolution.Authorized,
    adminDisplayName: String,
    months: List<PaymentMonth>,
    selectedMonth: PaymentMonth?,
    members: List<MemberPaymentStatus>,
    stats: List<ListeningStat>,
    whitelist: List<WhitelistMember>,
    allTimeWrapped: AllTimeWrappedSummary? = null,
    isBusy: Boolean,
    onSelectMonth: (String) -> Unit,
    onOpenAddMemberToMonth: (PaymentMonth) -> Unit,
    onDeleteMonth: (PaymentMonth) -> Unit,
    onShareMonthSummary: (PaymentMonth) -> Unit = {},
    onViewSlip: (MemberPaymentStatus) -> Unit,
    onConfirmPaid: (MemberPaymentStatus) -> Unit,
    onAdvancePayClick: (MemberPaymentStatus) -> Unit = {},
    onRewindPaid: (MemberPaymentStatus) -> Unit,
    onQuickStatusOverride: (MemberPaymentStatus, String) -> Unit = { _, _ -> },
    onEditPayment: (MemberPaymentStatus) -> Unit,
    onDeletePayment: (MemberPaymentStatus) -> Unit,
    onRemindMember: (MemberPaymentStatus, String) -> Unit,
) {
    val phoneByKey = remember(whitelist) {
        whitelist.associate { it.memberKey to it.phone }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Billing Archives",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Inspect past months, view All-Time Wrapped stats, and manage monthly billing summaries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            if (allTimeWrapped != null) {
                item {
                    AllTimeWrappedCard(summary = allTimeWrapped)
                }
            }

            if (months.isEmpty()) {
                item {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = "No archived months yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Past billing months will appear here once created.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(months, key = { it.monthId }) { month ->
                            val isSelected = month.monthId == selectedMonth?.monthId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectMonth(month.monthId) },
                                label = { Text(month.displayLabel) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("history_month_${month.monthId}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = SpotifyEmerald,
                                ),
                            )
                        }
                    }
                }

                if (selectedMonth != null) {
                    item {
                        val summary = remember(members) { calculateMonthlyBillingSummary(members) }
                        MonthlyBillingSummaryCard(
                            month = selectedMonth,
                            members = members,
                            summary = summary,
                            selfEmail = session.email,
                            isAdmin = session.isAdmin,
                            isBusy = isBusy,
                            onQuickStatusOverride = onQuickStatusOverride,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onShareMonthSummary(selectedMonth) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("history_share_summary_button"),
                                border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share_summary),
                                    tint = SpotifyEmerald,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_share_summary), color = SpotifyEmerald)
                            }
                            if (session.isAdmin) {
                                OutlinedButton(
                                    onClick = { onOpenAddMemberToMonth(selectedMonth) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("history_add_member_button"),
                                    border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp),
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
                                    onClick = { onDeleteMonth(selectedMonth) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("history_delete_month_button"),
                                    border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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

                    items(members, key = { "hist_${selectedMonth.monthId}_${it.memberKey}" }) { member ->
                        val isSelf = member.memberEmail.equals(session.email, ignoreCase = true)
                        val phone = phoneByKey[member.memberKey].orEmpty()
                        MemberPaymentCard(
                            member = member,
                            isSelf = isSelf,
                            isAdmin = session.isAdmin,
                            whatsappDigits = phone,
                            isBusy = isBusy,
                            modifier = Modifier.animateItem(),
                            onUploadSlipClick = {},
                            onViewSlipClick = { onViewSlip(member) },
                            onMarkPaidClick = { onConfirmPaid(member) },
                            onAdvancePayClick = { onAdvancePayClick(member) },
                            onRewindPaidClick = { onRewindPaid(member) },
                            onEditPaymentClick = { onEditPayment(member) },
                            onDeletePaymentClick = { onDeletePayment(member) },
                            onRemindClick = { onRemindMember(member, phone) },
                        )
                    }

                    if (leaderboardRoster.isNotEmpty()) {
                        val topHistMinutes = leaderboardRoster.maxOfOrNull { it.minutesListened }?.coerceAtLeast(1L) ?: 1L
                        item {
                            Text(
                                text = "Leaderboard (${selectedMonth.displayLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .animateItem(),
                            )
                        }
                        items(
                            leaderboardRoster.withIndex().toList(),
                            key = { "hist_stat_${selectedMonth.monthId}_${it.value.memberKey}" },
                        ) { (idx, stat) ->
                            ListeningLeaderboardCard(
                                rank = idx + 1,
                                stat = stat,
                                maxMinutes = topHistMinutes,
                                isSelf = stat.memberEmail.equals(session.email, ignoreCase = true),
                                isAdminEntry = stat.memberEmail.equals(AtaPotifyConstants.ADMIN_EMAIL, ignoreCase = true),
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllTimeWrappedCard(summary: AllTimeWrappedSummary) {
    val animatedMinutes by animateIntAsState(
        targetValue = summary.totalGroupMinutes.toInt(),
        animationSpec = tween(durationMillis = 650),
        label = "wrapped_minutes",
    )
    val animatedTracks by animateIntAsState(
        targetValue = summary.totalGroupTracks.toInt(),
        animationSpec = tween(durationMillis = 650),
        label = "wrapped_tracks",
    )
    val totalHours = remember(summary.totalGroupMinutes) {
        String.format(java.util.Locale.US, "%.1f", summary.totalGroupMinutes / 60.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("all_time_wrapped_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF133822), SurfaceDark),
                    )
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SpotifyEmerald,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.title_wrapped_summary),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Across ${summary.monthsTracked} synced billing month(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoftText,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Total Group Listening",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = "$animatedMinutes min",
                                style = MonoNumericLarge.copy(fontSize = 17.sp),
                                color = SpotifyEmerald,
                            )
                            Text(
                                text = "$totalHours hrs • $animatedTracks tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }

                    summary.topListener?.let { top ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "#1 Top Listener",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = top.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${top.totalMinutes} min • ${top.totalTracks} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (summary.topArtists.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        summary.topArtists.forEach { artist ->
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "Top Artist: $artist",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


