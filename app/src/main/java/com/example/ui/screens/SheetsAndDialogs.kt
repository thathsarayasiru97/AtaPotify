package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.FamilyGroup
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.UserProfile
import com.example.data.model.WhitelistMember
import com.example.data.model.resolveEffectiveFamilyGroups
import com.example.data.repository.AccessResolution
import com.example.data.service.AppUpdateService
import com.example.data.service.GitHubAppRelease
import com.example.data.service.ImageCompressor
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.data.model.buildReceiptReferenceCode
import com.example.ui.components.AnimatedStreakBadge
import com.example.ui.theme.EmeraldSoftText
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.parseGroupColorHex
import com.example.ui.viewmodel.OverdueReminderCandidate
import com.example.ui.viewmodel.ReceiptDialogState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorSheet(
    session: AccessResolution.Authorized,
    profile: UserProfile?,
    spotifyConnected: Boolean,
    spotifyName: String,
    notificationsEnabled: Boolean = true,
    onEnableNotifications: () -> Unit = {},
    isBusy: Boolean,
    onSaveProfile: (String, String, String) -> Unit,
    onDisconnectSpotify: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember(profile, session) {
        mutableStateOf(profile?.displayName?.takeIf { it.isNotBlank() } ?: session.displayName)
    }
    var phone by remember(profile, session) {
        mutableStateOf(profile?.phone ?: session.phone)
    }
    var spotifyEmail by remember(profile, session) {
        mutableStateOf(profile?.spotifyEmail?.takeIf { it.isNotBlank() } ?: session.spotifyEmail)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = session.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (session.isAdmin) "Admin" else "Member",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotifyEmerald,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_display_name_input"),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                label = { Text("WhatsApp Number (e.g. 94771234567)") },
                supportingText = { Text("Local 077... numbers are normalized to 9477... automatically.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input"),
            )

            OutlinedTextField(
                value = spotifyEmail,
                onValueChange = { spotifyEmail = it.trim() },
                label = { Text("Email Assigned to Spotify") },
                supportingText = { Text("Shared with the plan Admin for Spotify Family & leaderboard access.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_spotify_email_input"),
            )

            Button(
                onClick = { onSaveProfile(displayName, phone, spotifyEmail) },
                enabled = !isBusy && displayName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_profile))
            }

            Surface(
                color = if (notificationsEnabled) EmeraldContainer.copy(alpha = 0.45f) else AmberContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (notificationsEnabled) SpotifyEmerald.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.45f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (notificationsEnabled) SpotifyEmerald else AmberWarning,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                text = if (notificationsEnabled) {
                                    "Cycle Reminders Active"
                                } else {
                                    "Cycle Reminders Off"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (notificationsEnabled) SpotifyEmerald else AmberWarning,
                            )
                            Text(
                                text = if (notificationsEnabled) {
                                    "Alerts when a new billing cycle starts & payment is due"
                                } else {
                                    "Enable notifications for new cycle & due alerts"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    if (!notificationsEnabled) {
                        TextButton(
                            onClick = onEnableNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("profile_enable_notifications_button"),
                        ) {
                            Text("Enable", color = AmberWarning, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (spotifyConnected) {
                OutlinedButton(
                    onClick = onDisconnectSpotify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = SpotifyEmerald,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (spotifyName.isNotBlank()) "Disconnect Spotify ($spotifyName)"
                        else stringResource(R.string.btn_disconnect_spotify),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_check_updates_button"),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = SpotifyEmerald,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.btn_check_updates)} (v${BuildConfig.VERSION_NAME})",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_sign_out_button"),
                border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = CoralPending,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_sign_out), color = CoralPending)
            }
        }
    }
}

@Composable
fun ReceiptSlipDialog(
    state: ReceiptDialogState,
    isAdmin: Boolean,
    isBusy: Boolean,
    onConfirmPayment: () -> Unit,
    onRejectToPending: () -> Unit,
    onRewindPaid: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(22.dp),
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
                    Column {
                        Text(
                            text = "${state.member.displayName}'s Receipt",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${state.monthId} • LKR ${state.member.amountLkr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = TextSecondary,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBg),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(color = SpotifyEmerald)
                        !state.error.isNullOrBlank() -> Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                        state.slip != null -> {
                            val bmp = remember(state.slip.imageBase64) {
                                ImageCompressor.decodeBase64ToImageBitmap(state.slip.imageBase64)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Uploaded payment slip for ${state.member.displayName}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text("Could not decode receipt image", color = CoralPending)
                            }
                        }
                    }
                }

                if (isAdmin && state.member.status != AtaPotifyConstants.STATUS_PAID) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRejectToPending,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("reject_slip_button"),
                            border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.btn_reject_pending), color = CoralPending)
                        }

                        Button(
                            onClick = onConfirmPayment,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("confirm_payment_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_confirm_payment))
                        }
                    }
                } else if (isAdmin && state.member.status == AtaPotifyConstants.STATUS_PAID) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRewindPaid,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_rewind_paid_button"),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_rewind_paid), color = AmberWarning)
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoReminderQueueSheet(
    candidates: List<OverdueReminderCandidate>,
    onSendReminder: (OverdueReminderCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AmberWarning,
                )
                Column {
                    Text(
                        text = "7+ Day Overdue Auto-Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tap Send WhatsApp to launch each pre-filled message and record the reminder cooldown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(candidates, key = { it.member.memberKey }) { candidate ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = candidate.member.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Surface(color = AmberContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${candidate.pendingDays}d pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Text(
                                text = candidate.prefilledMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                            Button(
                                onClick = { onSendReminder(candidate) },
                                enabled = candidate.phoneDigits.length >= 9,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("auto_send_wa_${candidate.member.memberKey}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotifyEmerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (candidate.phoneDigits.length >= 9) {
                                        "Send via WhatsApp (+${candidate.phoneDigits})"
                                    } else {
                                        "No WhatsApp Number Saved"
                                    }
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
fun StartNewMonthDialog(
    suggestedMonthId: String,
    defaultAmountLkr: Long,
    activeMemberCount: Int,
    familyGroups: List<FamilyGroup> = emptyList(),
    activeCountByGroup: Map<String, Int> = emptyMap(),
    isBusy: Boolean,
    onConfirm: (monthId: String, label: String, amountLkr: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedGroups = remember(familyGroups, defaultAmountLkr) {
        resolveEffectiveFamilyGroups(familyGroups, defaultAmountLkr)
    }
    val breakdown = remember(resolvedGroups, activeCountByGroup) {
        resolvedGroups.joinToString(", ") { grp ->
            "${grp.name}: ${activeCountByGroup[grp.effectiveId] ?: 0}"
        }
    }
    var monthId by remember(suggestedMonthId) { mutableStateOf(suggestedMonthId) }
    var displayLabel by remember(suggestedMonthId) {
        mutableStateOf(AtaPotifyConstants.monthLabelFromId(suggestedMonthId))
    }
    var amountText by remember(defaultAmountLkr) { mutableStateOf(defaultAmountLkr.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Start New Billing Month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Initializes $activeMemberCount active friend(s) ($breakdown) as Pending using each group's configured split rate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = monthId,
                    onValueChange = {
                        monthId = it.trim()
                        if (it.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) {
                            displayLabel = AtaPotifyConstants.monthLabelFromId(it)
                        }
                    },
                    label = { Text("Month Key (YYYY-MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = displayLabel,
                    onValueChange = { displayLabel = it },
                    label = { Text("Display Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Share Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            val validMonth = monthId.matches(Regex("^[0-9]{4}-[0-9]{2}$"))
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onConfirm(monthId, displayLabel, amount) },
                enabled = !isBusy && validMonth && displayLabel.isNotBlank() && amount in 1..100_000,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Create Month")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhitelistMemberDialog(
    editingMember: WhitelistMember?,
    familyGroups: List<FamilyGroup> = emptyList(),
    defaultFamilyGroup: String = AtaPotifyConstants.GROUP_1,
    isBusy: Boolean,
    onSave: (email: String, name: String, phone: String, spotifyEmail: String, active: Boolean, familyGroup: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedGroups = remember(familyGroups) { resolveEffectiveFamilyGroups(familyGroups) }
    var email by remember(editingMember) { mutableStateOf(editingMember?.email.orEmpty()) }
    var name by remember(editingMember) { mutableStateOf(editingMember?.displayName.orEmpty()) }
    var phone by remember(editingMember) { mutableStateOf(editingMember?.phone.orEmpty()) }
    var spotifyEmail by remember(editingMember) { mutableStateOf(editingMember?.spotifyEmail.orEmpty()) }
    var active by remember(editingMember) { mutableStateOf(editingMember?.active ?: true) }
    var familyGroup by remember(editingMember, defaultFamilyGroup) {
        mutableStateOf(editingMember?.normalizedGroup ?: defaultFamilyGroup)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(if (editingMember == null) "Add Friend to Whitelist" else "Edit Friend")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("whitelist_name_input"),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Account Email") },
                    enabled = editingMember == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("whitelist_email_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                    label = { Text("WhatsApp Number (e.g. 94771234567)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("whitelist_phone_input"),
                )
                OutlinedTextField(
                    value = spotifyEmail,
                    onValueChange = { spotifyEmail = it.trim() },
                    label = { Text("Spotify Account Email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("whitelist_spotify_email_input"),
                )
                Text(
                    text = "Spotify Family Group",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                DynamicFamilyGroupChips(
                    familyGroups = resolvedGroups,
                    selectedGroupId = familyGroup,
                    onSelectGroup = { familyGroup = it },
                    testTagPrefix = "whitelist_group",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active in Family Plan", color = TextSecondary)
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = SpotifyEmerald,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(email, name, phone, spotifyEmail, active, familyGroup) },
                enabled = !isBusy && name.isNotBlank() && email.contains("@"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
fun SpotifySetupHintDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Configure Spotify Client ID") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "To activate one-tap Spotify OAuth login for your friends:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "1. Open the Secrets panel in AI Studio and add SPOTIFY_CLIENT_ID with your Spotify Developer Client ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "2. Ensure atapotify://spotify-callback is listed under Redirect URIs and your friends' Spotify emails are added under User Management in the Spotify Developer Dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Got it")
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPaymentDialog(
    member: MemberPaymentStatus,
    monthLabel: String,
    familyGroups: List<FamilyGroup> = emptyList(),
    initialFamilyGroup: String = member.normalizedGroup,
    isBusy: Boolean,
    onSave: (
        displayName: String,
        status: String,
        amountLkr: Long,
        clearReceipt: Boolean,
        resetReminders: Boolean,
        familyGroup: String,
        paidAmountLkr: Long,
        paymentMethod: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedGroups = remember(familyGroups) { resolveEffectiveFamilyGroups(familyGroups) }
    var displayName by remember(member) { mutableStateOf(member.displayName) }
    var status by remember(member) { mutableStateOf(member.status) }
    var amountText by remember(member) { mutableStateOf(member.amountLkr.toString()) }
    var paidAmountText by remember(member) { mutableStateOf(member.effectivePaidLkr.toString()) }
    var paymentMethod by remember(member) { mutableStateOf(member.paymentMethod) }
    var clearReceipt by remember(member) { mutableStateOf(false) }
    var resetReminders by remember(member) { mutableStateOf(false) }
    var familyGroup by remember(member, initialFamilyGroup) {
        mutableStateOf(AtaPotifyConstants.normalizeFamilyGroup(initialFamilyGroup))
    }

    val statusOptions = listOf(
        AtaPotifyConstants.STATUS_PENDING to stringResource(R.string.status_pending),
        AtaPotifyConstants.STATUS_VERIFYING to stringResource(R.string.status_verifying),
        AtaPotifyConstants.STATUS_PAID to stringResource(R.string.status_paid),
    )
    val methodOptions = listOf(
        AtaPotifyConstants.PAYMENT_METHOD_CASH to "Cash in Hand",
        AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP to "Bank Slip",
        AtaPotifyConstants.PAYMENT_METHOD_DIRECT to "Direct",
    )

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val parsedPaidSoFar = paidAmountText.toLongOrNull() ?: 0L
    val showMethodPicker = status == AtaPotifyConstants.STATUS_PAID || parsedPaidSoFar > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Edit Payment Log") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "$monthLabel • ${member.memberEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_payment_name_input"),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Share Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_payment_amount_input"),
                )
                Text(
                    text = "Family Group",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                DynamicFamilyGroupChips(
                    familyGroups = resolvedGroups,
                    selectedGroupId = familyGroup,
                    onSelectGroup = { familyGroup = it },
                    testTagPrefix = "edit_payment_group",
                )
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    statusOptions.forEach { (value, label) ->
                        val selected = status == value
                        FilterChip(
                            selected = selected,
                            onClick = { status = value },
                            label = { Text(label) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_payment_status_$value"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = SpotifyEmerald,
                            ),
                        )
                    }
                }
                if (status != AtaPotifyConstants.STATUS_PAID) {
                    val maxPartial = (parsedAmount - 1L).coerceAtLeast(0L)
                    val previewPaid = parsedPaidSoFar.coerceIn(0L, maxPartial)
                    val previewRemaining = (parsedAmount - previewPaid).coerceAtLeast(0L)
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Paid So Far (LKR)") },
                        supportingText = {
                            Text("0 to $maxPartial LKR for partial payment • Remaining: LKR $previewRemaining")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_payment_paid_amount_input"),
                    )
                }
                if (showMethodPicker) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        methodOptions.forEach { (methodValue, methodLabel) ->
                            val selected = paymentMethod == methodValue
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    paymentMethod = if (selected) "" else methodValue
                                },
                                label = { Text(methodLabel) },
                                modifier = Modifier.testTag("edit_payment_method_$methodValue"),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = SpotifyEmerald,
                                ),
                            )
                        }
                    }
                }
                if (member.hasReceipt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Remove attached receipt slip",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = clearReceipt,
                            onCheckedChange = { clearReceipt = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = CoralPending,
                            ),
                        )
                    }
                }
                if (member.reminderCount > 0L || member.lastRemindedAt != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Reset reminder count (${member.reminderCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = resetReminders,
                            onCheckedChange = { resetReminders = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = AmberWarning,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amount = parsedAmount
            val maxPartial = (amount - 1L).coerceAtLeast(0L)
            val paidToSave = if (status == AtaPotifyConstants.STATUS_PAID) {
                amount
            } else {
                parsedPaidSoFar.coerceIn(0L, maxPartial)
            }
            val methodToSave = if (status != AtaPotifyConstants.STATUS_PAID && paidToSave == 0L) {
                ""
            } else {
                paymentMethod
            }
            Button(
                onClick = {
                    onSave(
                        displayName,
                        status,
                        amount,
                        clearReceipt,
                        resetReminders,
                        familyGroup,
                        paidToSave,
                        methodToSave,
                    )
                },
                enabled = !isBusy && displayName.isNotBlank() && amount in 1..100_000,
                modifier = Modifier.testTag("edit_payment_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartialPaymentDialog(
    member: MemberPaymentStatus,
    monthLabel: String,
    isBusy: Boolean,
    onConfirmPartial: (additionalAmountLkr: Long, isCash: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val totalLkr = member.amountLkr.coerceAtLeast(1L)
    val paidSoFar = member.effectivePaidLkr
    val remainingLkr = member.remainingLkr.coerceAtLeast(1L)

    var amountText by remember(member) {
        mutableStateOf((remainingLkr / 2L).coerceAtLeast(1L).toString())
    }
    var isCash by remember(member) {
        mutableStateOf(member.paymentMethod != AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP)
    }

    val enteredAmount = amountText.toLongOrNull() ?: 0L
    val isValidAmount = enteredAmount in 1L..remainingLkr
    val projectedPaid = (paidSoFar + enteredAmount.coerceAtLeast(0L)).coerceAtMost(totalLkr)
    val projectedRemaining = (totalLkr - projectedPaid).coerceAtLeast(0L)
    val willSettle = enteredAmount >= remainingLkr && enteredAmount > 0L

    val quickAmounts = remember(remainingLkr) {
        buildList {
            val half = (remainingLkr / 2L).coerceAtLeast(1L)
            add("50% ($half)" to half)
            if (remainingLkr >= 100L && 100L != half && 100L < remainingLkr) {
                add("LKR 100" to 100L)
            }
            if (remainingLkr >= 200L && 200L != half && 200L < remainingLkr) {
                add("LKR 200" to 200L)
            }
            add("Full ($remainingLkr)" to remainingLkr)
        }.distinctBy { it.second }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        modifier = Modifier.testTag("partial_payment_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = AmberWarning,
                )
                Column {
                    Text(stringResource(R.string.btn_partial_payment))
                    Text(
                        text = "${member.displayName} • $monthLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                    ) {
                        Column {
                            Text("Share", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("LKR $totalLkr", style = MonoNumericSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Paid So Far", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("LKR $paidSoFar", style = MonoNumericSmall, color = SpotifyEmerald)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("LKR $remainingLkr", style = MonoNumericSmall, color = AmberWarning)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickAmounts.forEach { (chipLabel, chipAmount) ->
                        FilterChip(
                            selected = enteredAmount == chipAmount,
                            onClick = { amountText = chipAmount.toString() },
                            label = { Text(chipLabel) },
                            modifier = Modifier.testTag("partial_quick_amount_$chipAmount"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = AmberContainer,
                                selectedLabelColor = AmberWarning,
                            ),
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount Received Now (LKR)") },
                    supportingText = {
                        if (enteredAmount > remainingLkr) {
                            Text("Max LKR $remainingLkr remaining", color = CoralPending)
                        } else {
                            Text("Will add to existing LKR $paidSoFar paid")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("partial_payment_amount_input"),
                )

                Text(
                    text = "Received Via",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = isCash,
                        onClick = { isCash = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text("Cash in Hand") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("partial_payment_method_cash"),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary,
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = SpotifyEmerald,
                        ),
                    )
                    FilterChip(
                        selected = !isCash,
                        onClick = { isCash = false },
                        label = { Text("Bank Transfer") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("partial_payment_method_bank"),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary,
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = SpotifyEmerald,
                        ),
                    )
                }

                Surface(
                    color = if (willSettle) EmeraldContainer.copy(alpha = 0.5f) else AmberContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (willSettle) SpotifyEmerald.copy(alpha = 0.45f) else AmberWarning.copy(alpha = 0.45f),
                    ),
                ) {
                    Text(
                        text = when {
                            !isValidAmount -> "Enter an amount between 1 and $remainingLkr LKR."
                            willSettle -> "Settles full LKR $totalLkr balance → Member will be marked Paid."
                            else -> "New Paid Total: LKR $projectedPaid / $totalLkr • LKR $projectedRemaining left."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (willSettle) SpotifyEmerald else AmberWarning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPartial(enteredAmount, isCash) },
                enabled = !isBusy && isValidAmount,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("partial_payment_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (willSettle) SpotifyEmerald else AmberWarning,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (willSettle) "Settle & Mark Paid" else "Record LKR $enteredAmount")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMemberToMonthDialog(
    month: PaymentMonth,
    existingMembers: List<MemberPaymentStatus>,
    whitelist: List<WhitelistMember>,
    familyGroups: List<FamilyGroup> = emptyList(),
    adminDisplayName: String,
    isBusy: Boolean,
    onAdd: (email: String, displayName: String, amountLkr: Long, familyGroup: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedGroups = remember(familyGroups, month.amountLkr) {
        resolveEffectiveFamilyGroups(familyGroups, month.amountLkr)
    }
    val existingKeys = remember(existingMembers) {
        existingMembers.map { it.memberKey }.toSet()
    }
    val adminKey = remember { AtaPotifyConstants.memberKeyForEmail(AtaPotifyConstants.ADMIN_EMAIL) }

    val quickPickOptions = remember(whitelist, existingKeys, adminDisplayName) {
        buildList {
            if (adminKey !in existingKeys) {
                add(Triple(AtaPotifyConstants.ADMIN_EMAIL, "${adminDisplayName.ifBlank { "Admin" }} (Admin)", AtaPotifyConstants.GROUP_1))
            }
            whitelist.filter { it.memberKey !in existingKeys }.forEach { w ->
                add(Triple(w.email, w.displayName, w.normalizedGroup))
            }
        }
    }

    var email by remember(month.monthId) { mutableStateOf("") }
    var displayName by remember(month.monthId) { mutableStateOf("") }
    var familyGroup by remember(month.monthId) {
        mutableStateOf(resolvedGroups.firstOrNull()?.effectiveId ?: AtaPotifyConstants.GROUP_1)
    }
    var hasCustomAmount by remember(month.monthId) { mutableStateOf(false) }
    var amountText by remember(month.monthId) {
        val initialRate = resolvedGroups.firstOrNull { it.effectiveId == familyGroup }?.amountLkr ?: month.amountLkr
        mutableStateOf(initialRate.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = SpotifyEmerald,
                )
                Text("Add to ${month.displayLabel}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (quickPickOptions.isNotEmpty()) {
                    Text(
                        text = "Quick pick someone not yet in this month:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        quickPickOptions.forEach { (optEmail, optLabel, optGroup) ->
                            val cleanName = optLabel.removeSuffix(" (Admin)")
                            FilterChip(
                                selected = email.equals(optEmail, ignoreCase = true),
                                onClick = {
                                    email = optEmail
                                    displayName = cleanName
                                    familyGroup = optGroup
                                    if (!hasCustomAmount) {
                                        val grpRate = resolvedGroups.firstOrNull { it.effectiveId == optGroup }?.amountLkr
                                        if (grpRate != null) {
                                            amountText = grpRate.toString()
                                        }
                                    }
                                },
                                label = { Text(optLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = SpotifyEmerald,
                                ),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_to_month_name_input"),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_to_month_email_input"),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        hasCustomAmount = true
                        amountText = it.filter { c -> c.isDigit() }
                    },
                    label = { Text("Share Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_to_month_amount_input"),
                )
                DynamicFamilyGroupChips(
                    familyGroups = resolvedGroups,
                    selectedGroupId = familyGroup,
                    onSelectGroup = { newGroup ->
                        familyGroup = newGroup
                        if (!hasCustomAmount) {
                            val grpRate = resolvedGroups.firstOrNull { it.effectiveId == newGroup }?.amountLkr
                            if (grpRate != null) {
                                amountText = grpRate.toString()
                            }
                        }
                    },
                    testTagPrefix = "add_to_month_group",
                )
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onAdd(email, displayName, amount, familyGroup) },
                enabled = !isBusy && displayName.isNotBlank() && email.contains("@") && amount in 1..100_000,
                modifier = Modifier.testTag("add_to_month_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = CoralPending,
                )
                Text(title)
            }
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy,
                modifier = Modifier.testTag("confirm_delete_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPending,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
fun AppUpdateAvailableDialog(
    release: GitHubAppRelease,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sizeText = remember(release.apkSizeBytes) {
        AppUpdateService.formatApkSize(release.apkSizeBytes)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = SpotifyEmerald,
                )
                Column {
                    Text(
                        text = "Update Available (${release.tagName})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (release.title.isNotBlank() && release.title != release.tagName) {
                        Text(
                            text = release.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!release.apkFileName.isNullOrBlank()) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        val badge = if (sizeText.isNotBlank()) {
                            "${release.apkFileName} • $sizeText"
                        } else {
                            release.apkFileName
                        }
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotifyEmerald,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
                Text(
                    text = release.releaseNotes.ifBlank {
                        "A newer version of AtaPotify is ready on GitHub Releases."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("app_update_download_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (release.apkDownloadUrl != null) {
                        stringResource(R.string.btn_download_update)
                    } else {
                        "Open Release"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("app_update_later_button"),
            ) {
                Text("Later", color = TextSecondary)
            }
        },
    )
}

@Composable
fun SpotifyEmailOnboardingDialog(
    googleEmail: String,
    isBusy: Boolean,
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
) {
    var spotifyEmail by remember(googleEmail) { mutableStateOf(googleEmail) }

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = SpotifyEmerald,
                )
                Text("Your Spotify Email")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Welcome to AtaPotify! Please enter the email address assigned to your Spotify account. This will be reported to the Admin panel so your account can be linked to the Spotify Family Plan & Leaderboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                FilterChip(
                    selected = spotifyEmail.equals(googleEmail, ignoreCase = true),
                    onClick = { spotifyEmail = googleEmail },
                    label = { Text("Same as Google ($googleEmail)") },
                    modifier = Modifier.testTag("spotify_email_same_as_google_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        labelColor = TextSecondary,
                        selectedContainerColor = EmeraldContainer,
                        selectedLabelColor = SpotifyEmerald,
                    ),
                )
                OutlinedTextField(
                    value = spotifyEmail,
                    onValueChange = { spotifyEmail = it.trim() },
                    label = { Text("Email Assigned to Spotify") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_spotify_email_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(spotifyEmail) },
                enabled = !isBusy && spotifyEmail.contains("@") && spotifyEmail.length >= 3,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("onboarding_spotify_email_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Submit to Admin")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isBusy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("onboarding_spotify_email_skip_button"),
            ) {
                Text("Later", color = TextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancePaymentDialog(
    member: MemberPaymentStatus,
    startMonth: PaymentMonth,
    isBusy: Boolean,
    onConfirmAdvance: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val monthOptions = listOf(2, 3, 6, 12)
    var selectedMonths by remember { mutableStateOf(3) }
    val targetMonthIds = remember(startMonth.monthId, selectedMonths) {
        AtaPotifyConstants.consecutiveMonthIds(startMonth.monthId, selectedMonths)
    }
    val totalLkr = member.amountLkr * selectedMonths

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SpotifyEmerald,
                )
                Text("Multi-Month Advance Pay")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mark ${member.displayName} as Paid starting from ${startMonth.displayLabel} across consecutive future months (future months will be auto-created if needed).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    monthOptions.forEach { count ->
                        val selected = selectedMonths == count
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMonths = count },
                            label = { Text("$count Months") },
                            modifier = Modifier.testTag("advance_months_chip_$count"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = SpotifyEmerald,
                            ),
                        )
                    }
                }
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Coverage: ${targetMonthIds.first()} → ${targetMonthIds.last()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SpotifyEmerald,
                        )
                        Text(
                            text = "Total Share Covered: LKR $totalLkr ($selectedMonths × LKR ${member.amountLkr})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAdvance(selectedMonths) },
                enabled = !isBusy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_advance_pay_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Confirm $selectedMonths Months Paid")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
fun VerifiedPaymentReceiptDialog(
    month: PaymentMonth,
    member: MemberPaymentStatus,
    streakCount: Int = 0,
    canViewSlip: Boolean = false,
    onShareReceipt: () -> Unit,
    onViewUploadedSlip: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val refCode = remember(month.monthId, member.memberKey) {
        buildReceiptReferenceCode(month.monthId, member.memberKey)
    }
    val verifiedDateStr = remember(member.confirmedAt, member.updatedAt) {
        val ts = member.confirmedAt ?: member.updatedAt
        if (ts != null) {
            SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(ts.toDate())
        } else {
            "Verified by Admin"
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val sealScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "verified_seal_scale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "receipt_glow_anim")
    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "receipt_border_glow",
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verified_payment_receipt_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, SpotifyEmerald.copy(alpha = borderGlowAlpha)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF143823), SurfaceDark, ObsidianBg),
                        )
                    )
                    .padding(20.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = refCode,
                                style = MonoNumericSmall.copy(fontSize = 11.sp),
                                color = SpotifyEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = TextSecondary,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(sealScale)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Payment Seal",
                            tint = SpotifyEmerald,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "VERIFIED PAYMENT RECEIPT",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSoftText,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "LKR ${member.amountLkr}",
                            style = MonoNumericLarge.copy(fontSize = 28.sp),
                            color = SpotifyEmerald,
                        )
                        if (streakCount >= 1) {
                            Spacer(Modifier.height(2.dp))
                            AnimatedStreakBadge(streakCount = streakCount)
                        }
                    }

                    Surface(
                        color = SurfaceElevated.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReceiptMetaRow(label = "Member", value = member.displayName)
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptMetaRow(label = "Billing Cycle", value = "${month.displayLabel} (${month.monthId})")
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptMetaRow(
                                label = "Verification",
                                value = when {
                                    member.isCashPayment -> "Cash in Hand (Paid to Admin)"
                                    member.hasReceipt -> "Bank Slip Verified"
                                    else -> "Confirmed by Admin"
                                },
                            )
                            HorizontalDivider(color = BorderSubtle)
                            ReceiptMetaRow(label = "Confirmed On", value = verifiedDateStr)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (canViewSlip && member.hasReceipt) {
                            OutlinedButton(
                                onClick = onViewUploadedSlip,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("verified_receipt_view_slip_button"),
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Bank Slip", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Button(
                            onClick = onShareReceipt,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("verified_receipt_share_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Share Receipt")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DynamicFamilyGroupChips(
    familyGroups: List<FamilyGroup>,
    selectedGroupId: String,
    onSelectGroup: (String) -> Unit,
    testTagPrefix: String,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        familyGroups.forEach { grp ->
            val accent = parseGroupColorHex(grp.colorHex, SpotifyEmerald)
            FilterChip(
                selected = selectedGroupId == grp.effectiveId,
                onClick = { onSelectGroup(grp.effectiveId) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                },
                label = { Text(grp.name) },
                modifier = Modifier.testTag("${testTagPrefix}_${grp.effectiveId}"),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceElevated,
                    labelColor = TextSecondary,
                    selectedContainerColor = accent.copy(alpha = 0.2f),
                    selectedLabelColor = accent,
                ),
            )
        }
    }
}


