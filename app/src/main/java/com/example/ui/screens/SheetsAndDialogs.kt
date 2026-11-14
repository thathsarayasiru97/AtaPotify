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
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.UserProfile
import com.example.data.model.WhitelistMember
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
import com.example.ui.viewmodel.OverdueReminderCandidate
import com.example.ui.viewmodel.ReceiptDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorSheet(
    session: AccessResolution.Authorized,
    profile: UserProfile?,
    spotifyConnected: Boolean,
    spotifyName: String,
    isBusy: Boolean,
    onSaveProfile: (String, String) -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            Button(
                onClick = { onSaveProfile(displayName, phone) },
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
    isBusy: Boolean,
    onConfirm: (monthId: String, label: String, amountLkr: Long) -> Unit,
    onDismiss: () -> Unit,
) {
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
                    text = "Initializes $activeMemberCount active whitelisted friend(s) as Pending.",
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

@Composable
fun WhitelistMemberDialog(
    editingMember: WhitelistMember?,
    isBusy: Boolean,
    onSave: (email: String, name: String, phone: String, active: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var email by remember(editingMember) { mutableStateOf(editingMember?.email.orEmpty()) }
    var name by remember(editingMember) { mutableStateOf(editingMember?.displayName.orEmpty()) }
    var phone by remember(editingMember) { mutableStateOf(editingMember?.phone.orEmpty()) }
    var active by remember(editingMember) { mutableStateOf(editingMember?.active ?: true) }

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
                onClick = { onSave(email, name, phone, active) },
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

@Composable
fun EditPaymentDialog(
    member: MemberPaymentStatus,
    monthLabel: String,
    isBusy: Boolean,
    onSave: (
        displayName: String,
        status: String,
        amountLkr: Long,
        clearReceipt: Boolean,
        resetReminders: Boolean,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember(member) { mutableStateOf(member.displayName) }
    var status by remember(member) { mutableStateOf(member.status) }
    var amountText by remember(member) { mutableStateOf(member.amountLkr.toString()) }
    var clearReceipt by remember(member) { mutableStateOf(false) }
    var resetReminders by remember(member) { mutableStateOf(false) }

    val statusOptions = listOf(
        AtaPotifyConstants.STATUS_PENDING to stringResource(R.string.status_pending),
        AtaPotifyConstants.STATUS_VERIFYING to stringResource(R.string.status_verifying),
        AtaPotifyConstants.STATUS_PAID to stringResource(R.string.status_paid),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Edit Payment Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    label = { Text("Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_payment_amount_input"),
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
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(displayName, status, amount, clearReceipt, resetReminders) },
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
fun AddMemberToMonthDialog(
    month: PaymentMonth,
    existingMembers: List<MemberPaymentStatus>,
    whitelist: List<WhitelistMember>,
    adminDisplayName: String,
    isBusy: Boolean,
    onAdd: (email: String, displayName: String, amountLkr: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val existingKeys = remember(existingMembers) {
        existingMembers.map { it.memberKey }.toSet()
    }
    val adminKey = remember { AtaPotifyConstants.memberKeyForEmail(AtaPotifyConstants.ADMIN_EMAIL) }

    val quickPickOptions = remember(whitelist, existingKeys, adminDisplayName) {
        buildList {
            if (adminKey !in existingKeys) {
                add(AtaPotifyConstants.ADMIN_EMAIL to "${adminDisplayName.ifBlank { "Admin" }} (Admin)")
            }
            whitelist.filter { it.memberKey !in existingKeys }.forEach { w ->
                add(w.email to w.displayName)
            }
        }
    }

    var email by remember(month.monthId) { mutableStateOf("") }
    var displayName by remember(month.monthId) { mutableStateOf("") }
    var amountText by remember(month.monthId) { mutableStateOf(month.amountLkr.toString()) }

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
                        quickPickOptions.forEach { (optEmail, optLabel) ->
                            val cleanName = optLabel.removeSuffix(" (Admin)")
                            FilterChip(
                                selected = email.equals(optEmail, ignoreCase = true),
                                onClick = {
                                    email = optEmail
                                    displayName = cleanName
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
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Share Amount (LKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_to_month_amount_input"),
                )
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onAdd(email, displayName, amount) },
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

