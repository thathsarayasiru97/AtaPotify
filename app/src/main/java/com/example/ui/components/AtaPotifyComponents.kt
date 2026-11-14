package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.PaymentStatus
import com.example.data.model.UserProfile
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMuted
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MoneyCompactStyle
import com.example.ui.theme.MoneyDisplayStyle
import com.example.ui.theme.OverdueBannerBg
import com.example.ui.theme.OverdueBannerBorder
import com.example.ui.theme.PaidContainer
import com.example.ui.theme.PaidInk
import com.example.ui.theme.PendingContainer
import com.example.ui.theme.PendingInk
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceSlate
import com.example.ui.theme.VerifyingContainer
import com.example.ui.theme.VerifyingInk
import com.example.ui.viewmodel.OverdueMemberItem
import com.example.ui.viewmodel.ReceiptViewerState
import com.example.util.ImageCompressor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    AnimatedContent(targetState = status, label = "status_badge", modifier = modifier) { current ->
        val (bg, ink, icon) = when (current) {
            PaymentStatus.PAID -> Triple(PaidContainer, PaidInk, Icons.Filled.CheckCircle)
            PaymentStatus.VERIFYING -> Triple(VerifyingContainer, VerifyingInk, Icons.Filled.ReceiptLong)
            PaymentStatus.PENDING -> Triple(PendingContainer, PendingInk, Icons.Filled.Schedule)
        }
        Surface(
            color = bg,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, ink.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = current.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MemberAvatar(
    name: String,
    isPaid: Boolean,
    isSelf: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isPaid) EmeraldMuted else SurfaceElevated)
            .border(
                width = if (isPaid || isSelf) 1.5.dp else 1.dp,
                color = if (isPaid) EmeraldPrimary else if (isSelf) EmeraldDeep.copy(alpha = 0.6f) else BorderSubtle,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = if (isPaid) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HeroMonthSummaryCard(
    month: PaymentMonth,
    members: List<MemberPaymentStatus>,
    modifier: Modifier = Modifier
) {
    val totalCount = members.size
    val paidCount = members.count { it.paymentStatus == PaymentStatus.PAID }
    val verifyingCount = members.count { it.paymentStatus == PaymentStatus.VERIFYING }
    val pendingCount = members.count { it.paymentStatus == PaymentStatus.PENDING }
    val progress = if (totalCount > 0) paidCount.toFloat() / totalCount.toFloat() else 0f
    val collectedLkr = paidCount * month.amountLkr

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.16f),
                            SurfaceSlate
                        ),
                        radius = 750f
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "BILLING CYCLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = month.displayLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = EmeraldMuted,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "PER PERSON",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "LKR ${month.amountLkr}",
                                style = MoneyCompactStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "LKR $collectedLkr collected",
                            style = MoneyDisplayStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$paidCount / $totalCount paid",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = EmeraldPrimary,
                        trackColor = SurfaceElevated
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniCountChip("Paid", paidCount, PaidInk, PaidContainer, Modifier.weight(1f))
                    MiniCountChip("Verifying", verifyingCount, VerifyingInk, VerifyingContainer, Modifier.weight(1f))
                    MiniCountChip("Pending", pendingCount, PendingInk, PendingContainer, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniCountChip(
    label: String,
    count: Int,
    ink: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = count.toString(),
                style = MoneyCompactStyle,
                color = ink
            )
        }
    }
}

@Composable
fun OverdueAutoReminderBanner(
    overdueItems: List<OverdueMemberItem>,
    onOpenReminderQueue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("overdue_auto_reminder_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OverdueBannerBg),
        border = BorderStroke(1.dp, OverdueBannerBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = VerifyingInk
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${overdueItems.size} pending 7+ days",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Auto-reminder check detected overdue friends ready for a WhatsApp nudge.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss overdue reminder banner",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onOpenReminderQueue,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("open_auto_reminder_queue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerifyingInk,
                    contentColor = SurfaceSlate
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Send Overdue WhatsApp Reminders")
            }
        }
    }
}

@Composable
fun ReceiptViewerDialog(
    state: ReceiptViewerState,
    isAdmin: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (monthId: String, member: MemberPaymentStatus) -> Unit,
    onRejectToPending: (monthId: String, member: MemberPaymentStatus) -> Unit
) {
    if (state is ReceiptViewerState.Hidden) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceSlate,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val memberName = when (state) {
                        is ReceiptViewerState.Loading -> state.member.displayName
                        is ReceiptViewerState.Loaded -> state.member.displayName
                        is ReceiptViewerState.Error -> state.member.displayName
                        ReceiptViewerState.Hidden -> ""
                    }
                    Column {
                        Text(
                            text = "Payment Slip",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = memberName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close receipt viewer"
                        )
                    }
                }

                when (state) {
                    is ReceiptViewerState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }

                    is ReceiptViewerState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PendingInk
                        )
                    }

                    is ReceiptViewerState.Loaded -> {
                        val slip = state.slip
                        val bitmap = remember(slip?.imageBase64) {
                            slip?.imageBase64?.let { ImageCompressor.decodeBase64ToImageBitmap(it) }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Uploaded payment receipt for ${state.member.displayName}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceElevated),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No receipt image stored for this member.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val submittedDate = state.member.submittedAt?.toDate() ?: slip?.uploadedAt?.toDate()
                        if (submittedDate != null) {
                            Text(
                                text = "Uploaded ${formatShortDate(submittedDate)} • LKR ${state.member.amountLkr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isAdmin) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.member.paymentStatus != PaymentStatus.PENDING) {
                                    OutlinedButton(
                                        onClick = { onRejectToPending(state.monthId, state.member) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("dialog_reject_receipt_button"),
                                        border = BorderStroke(1.dp, PendingInk.copy(alpha = 0.6f))
                                    ) {
                                        Text("Reset Pending", color = PendingInk)
                                    }
                                }
                                if (state.member.paymentStatus != PaymentStatus.PAID) {
                                    Button(
                                        onClick = { onConfirmPayment(state.monthId, state.member) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("dialog_confirm_payment_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("Confirm Paid")
                                    }
                                }
                            }
                        }
                    }

                    ReceiptViewerState.Hidden -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorSheet(
    userEmail: String,
    isAdmin: Boolean,
    profile: UserProfile?,
    fallbackName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (displayName: String, phone: String) -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var displayName by remember(profile?.displayName, fallbackName) {
        mutableStateOf(profile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var phone by remember(profile?.phone) {
        mutableStateOf(profile?.phone.orEmpty())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceSlate
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (isAdmin) EmeraldMuted else SurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAdmin) EmeraldPrimary.copy(alpha = 0.4f) else BorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = if (isAdmin) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAdmin) "Admin" else "Member",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAdmin) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    validationError = null
                },
                label = { Text("Display Name") },
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_display_name_input")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    validationError = null
                },
                label = { Text("WhatsApp Number") },
                placeholder = { Text("e.g. 94771234567 or 0771234567") },
                supportingText = {
                    Text("Used by Admin for payment reminders. Stored privately in your profile.")
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input")
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = PendingInk
                )
            }

            Button(
                onClick = {
                    val trimmedName = displayName.trim()
                    if (trimmedName.isEmpty()) {
                        validationError = "Display name cannot be empty."
                    } else {
                        onSaveProfile(trimmedName, phone.trim())
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }

            HorizontalDivider(color = BorderSubtle)

            TextButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = PendingInk
                )
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = PendingInk)
            }
        }
    }
}

fun formatShortDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    return formatter.format(date)
}
