package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.MonoNumericSmall
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
    isBusy: Boolean,
    onSelectMonth: (String) -> Unit,
    onOpenAddMemberToMonth: (PaymentMonth) -> Unit,
    onDeleteMonth: (PaymentMonth) -> Unit,
    onViewSlip: (MemberPaymentStatus) -> Unit,
    onConfirmPaid: (MemberPaymentStatus) -> Unit,
    onRewindPaid: (MemberPaymentStatus) -> Unit,
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
                        text = "Inspect past months, edit payment logs, and check the Spotify Leaderboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = selectedMonth.displayLabel,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "${summary.paidCount} / ${summary.totalCount} paid • Pending LKR ${summary.totalPendingLkr}",
                                            style = MonoNumericSmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    Text(
                                        text = "LKR ${summary.collectedLkr}",
                                        style = MonoNumericLarge.copy(fontSize = 20.sp),
                                        color = SpotifyEmerald,
                                    )
                                }

                                if (session.isAdmin) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
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
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("history_delete_month_button"),
                                            border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.45f)),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = CoralPending,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.btn_delete_month), color = CoralPending)
                                        }
                                    }
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
                            onUploadSlipClick = {},
                            onViewSlipClick = { onViewSlip(member) },
                            onMarkPaidClick = { onConfirmPaid(member) },
                            onRewindPaidClick = { onRewindPaid(member) },
                            onEditPaymentClick = { onEditPayment(member) },
                            onDeletePaymentClick = { onDeletePayment(member) },
                            onRemindClick = { onRemindMember(member, phone) },
                        )
                    }

                    if (leaderboardRoster.isNotEmpty()) {
                        item {
                            Text(
                                text = "Leaderboard (${selectedMonth.displayLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        items(
                            leaderboardRoster.withIndex().toList(),
                            key = { "hist_stat_${selectedMonth.monthId}_${it.value.memberKey}" },
                        ) { (idx, stat) ->
                            ListeningLeaderboardCard(
                                rank = idx + 1,
                                stat = stat,
                                amountLkr = selectedMonth.amountLkr,
                                isSelf = stat.memberEmail.equals(session.email, ignoreCase = true),
                                isAdminEntry = stat.memberEmail.equals(AtaPotifyConstants.ADMIN_EMAIL, ignoreCase = true),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

