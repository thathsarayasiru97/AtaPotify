package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccessRequest
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.PaymentMonth
import com.example.data.model.PlanConfig
import com.example.data.model.WhitelistMember
import com.example.ui.components.FamilyGroupBadge
import com.example.ui.components.FamilyGroupFilterBar
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun AdminScreen(
    planConfig: PlanConfig,
    currentMonth: PaymentMonth?,
    whitelist: List<WhitelistMember>,
    pendingRequests: List<AccessRequest> = emptyList(),
    isBusy: Boolean,
    onApproveRequest: (AccessRequest, String) -> Unit = { _, _ -> },
    onDeclineRequest: (AccessRequest) -> Unit = {},
    onSaveLkrRate: (Long, Boolean) -> Unit,
    onSaveBankDetails: (bankName: String, accountNumber: String, accountHolder: String, branch: String) -> Unit = { _, _, _, _ -> },
    onSavePinnedAnnouncement: (String) -> Unit = {},
    onAddPinnedAnnouncement: (String) -> Unit = { onSavePinnedAnnouncement(it) },
    onRemovePinnedAnnouncementAt: (Int) -> Unit = {},
    onClearAllPinnedAnnouncements: () -> Unit = { onSavePinnedAnnouncement("") },
    onCopyText: (label: String, text: String) -> Unit = { _, _ -> },
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (WhitelistMember) -> Unit,
    onToggleMemberActive: (WhitelistMember, Boolean) -> Unit,
    onSwitchMemberGroup: (WhitelistMember, String) -> Unit = { _, _ -> },
    onRemoveMemberClick: (WhitelistMember) -> Unit,
) {
    var lkrInput by remember(planConfig.monthlyAmountLkr) {
        mutableStateOf(planConfig.monthlyAmountLkr.toString())
    }
    var alsoUpdateActive by remember { mutableStateOf(true) }

    var bankName by remember(planConfig.bankName) { mutableStateOf(planConfig.bankName) }
    var bankAccountNumber by remember(planConfig.bankAccountNumber) { mutableStateOf(planConfig.bankAccountNumber) }
    var bankAccountHolder by remember(planConfig.bankAccountHolder) { mutableStateOf(planConfig.bankAccountHolder) }
    var bankBranch by remember(planConfig.bankBranch) { mutableStateOf(planConfig.bankBranch) }
    var announcementInput by remember { mutableStateOf("") }
    var rosterGroupFilter by remember { mutableStateOf(AtaPotifyConstants.GROUP_ALL) }

    val activeSlides = planConfig.effectiveAnnouncements
    val activeGroup1Count = remember(whitelist) {
        whitelist.count { it.active && it.normalizedGroup == AtaPotifyConstants.GROUP_1 }
    }
    val activeGroup2Count = remember(whitelist) {
        whitelist.count { it.active && it.normalizedGroup == AtaPotifyConstants.GROUP_2 }
    }
    val defaultSuggestedGroup = if (activeGroup1Count >= AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS) {
        AtaPotifyConstants.GROUP_2
    } else {
        AtaPotifyConstants.GROUP_1
    }

    val filteredWhitelist = remember(whitelist, rosterGroupFilter) {
        when (rosterGroupFilter) {
            AtaPotifyConstants.GROUP_1 -> whitelist.filter { it.normalizedGroup == AtaPotifyConstants.GROUP_1 }
            AtaPotifyConstants.GROUP_2 -> whitelist.filter { it.normalizedGroup == AtaPotifyConstants.GROUP_2 }
            else -> whitelist
        }
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
            item(key = "admin_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Admin Controls",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Approve join requests into Family 1 or Family 2, manage pinned slideshow messages, bank details, and member rosters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            if (pendingRequests.isNotEmpty()) {
                item(key = "pending_requests_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.HowToReg,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Pending Approval Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberWarning,
                            )
                        }
                    }
                }

                items(pendingRequests, key = { "req_${it.uid.ifBlank { it.id }}" }) { request ->
                    AccessRequestCard(
                        request = request,
                        defaultGroup = defaultSuggestedGroup,
                        group1ActiveCount = activeGroup1Count,
                        group2ActiveCount = activeGroup2Count,
                        isBusy = isBusy,
                        modifier = Modifier.animateItem(),
                        onApprove = { targetGroup -> onApproveRequest(request, targetGroup) },
                        onDecline = { onDeclineRequest(request) },
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Monthly Split Amount (LKR)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedTextField(
                            value = lkrInput,
                            onValueChange = { input -> lkrInput = input.filter { it.isDigit() } },
                            label = { Text("Fixed Amount per Friend (LKR)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_lkr_input"),
                        )

                        if (currentMonth != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Also update ${currentMonth.displayLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                                Switch(
                                    checked = alsoUpdateActive,
                                    onCheckedChange = { alsoUpdateActive = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = SpotifyEmerald,
                                    ),
                                )
                            }
                        }

                        val parsed = lkrInput.toLongOrNull()
                        Button(
                            onClick = {
                                if (parsed != null && parsed in 1..100_000) {
                                    onSaveLkrRate(parsed, alsoUpdateActive)
                                }
                            },
                            enabled = !isBusy && parsed != null && parsed in 1..100_000,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("save_lkr_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Save LKR Rate")
                        }
                    }
                }
            }

            item(key = "pinned_announcement_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = SpotifyEmerald,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = "Pinned Board Announcements (5s Slideshow)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Add multiple pinned messages — they auto-rotate every 5 seconds on the Board.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "${activeSlides.size}/${AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SpotifyEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (activeSlides.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                activeSlides.forEachIndexed { index, slide ->
                                    Surface(
                                        color = SurfaceElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_pinned_slide_$index"),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SpotifyEmerald,
                                                )
                                            }
                                            Text(
                                                text = slide,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { onRemovePinnedAnnouncementAt(index) },
                                                enabled = !isBusy,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("remove_pinned_slide_$index"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove slide ${index + 1}",
                                                    tint = CoralPending,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = announcementInput,
                            onValueChange = { announcementInput = it.take(280) },
                            label = { Text("New Slideshow Message (e.g. Pay before the 5th!)") },
                            supportingText = {
                                Text("${announcementInput.length} / 280 chars • ${activeSlides.size} active slide(s)")
                            },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_announcement_input"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (activeSlides.isNotEmpty() || announcementInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        announcementInput = ""
                                        onClearAllPinnedAnnouncements()
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("clear_announcement_button"),
                                    border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Clear All", color = CoralPending)
                                }
                            }
                            Button(
                                onClick = {
                                    val clean = announcementInput.trim()
                                    if (clean.isNotEmpty()) {
                                        onAddPinnedAnnouncement(clean)
                                        announcementInput = ""
                                    }
                                },
                                enabled = !isBusy &&
                                    announcementInput.trim().isNotBlank() &&
                                    activeSlides.size < AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_announcement_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotifyEmerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (activeSlides.isEmpty()) "Pin to Board" else "Add Slide")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = SpotifyEmerald,
                                modifier = Modifier.size(20.dp),
                            )
                            Column {
                                Text(
                                    text = "Bank Transfer Details (for Friends)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Shown on the Board with a 1-tap Copy button for friends to pay their share.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Bank Name (e.g. Commercial Bank)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_bank_name_input"),
                            )
                            OutlinedTextField(
                                value = bankBranch,
                                onValueChange = { bankBranch = it },
                                label = { Text("Branch") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("admin_bank_branch_input"),
                            )
                        }

                        OutlinedTextField(
                            value = bankAccountNumber,
                            onValueChange = { bankAccountNumber = it },
                            label = { Text("Account Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_bank_account_input"),
                        )

                        OutlinedTextField(
                            value = bankAccountHolder,
                            onValueChange = { bankAccountHolder = it },
                            label = { Text("Account Holder Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_bank_holder_input"),
                        )

                        Button(
                            onClick = {
                                onSaveBankDetails(
                                    bankName,
                                    bankAccountNumber,
                                    bankAccountHolder,
                                    bankBranch,
                                )
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("save_bank_details_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Save Bank Details")
                        }
                    }
                }
            }

            item(key = "whitelist_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Approved Members (${filteredWhitelist.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            val reportedCount = whitelist.count { it.spotifyEmail.isNotBlank() }
                            Text(
                                text = "Fam 1: $activeGroup1Count/6 active • Fam 2: $activeGroup2Count/6 active • $reportedCount Spotify emails",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        Button(
                            onClick = onAddMemberClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_whitelist_member_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldContainer,
                                contentColor = SpotifyEmerald,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add Friend")
                        }
                    }

                    FamilyGroupFilterBar(
                        selectedGroup = rosterGroupFilter,
                        allCount = whitelist.size,
                        group1Count = activeGroup1Count,
                        group2Count = activeGroup2Count,
                        showSlotCapacity = true,
                        onSelectGroup = { rosterGroupFilter = it },
                    )
                }
            }

            if (filteredWhitelist.isEmpty()) {
                item(key = "whitelist_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = if (whitelist.isEmpty()) {
                                    "No approved members yet"
                                } else {
                                    "No members in ${AtaPotifyConstants.familyGroupLabel(rosterGroupFilter)} yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Approve pending join requests above, tap Add Friend, or switch a member's group badge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            } else {
                items(filteredWhitelist, key = { it.memberKey }) { member ->
                    val nextGroup = if (member.normalizedGroup == AtaPotifyConstants.GROUP_2) {
                        AtaPotifyConstants.GROUP_1
                    } else {
                        AtaPotifyConstants.GROUP_2
                    }
                    WhitelistRosterCard(
                        member = member,
                        isBusy = isBusy,
                        modifier = Modifier.animateItem(),
                        onCopySpotifyEmail = {
                            if (member.spotifyEmail.isNotBlank()) {
                                onCopyText("Spotify Email", member.spotifyEmail)
                            }
                        },
                        onSwitchGroup = { onSwitchMemberGroup(member, nextGroup) },
                        onEdit = { onEditMemberClick(member) },
                        onToggleActive = { active -> onToggleMemberActive(member, active) },
                        onRemove = { onRemoveMemberClick(member) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AccessRequestCard(
    request: AccessRequest,
    defaultGroup: String,
    group1ActiveCount: Int,
    group2ActiveCount: Int,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onApprove: (String) -> Unit,
    onDecline: () -> Unit,
) {
    val reqUid = request.uid.ifBlank { request.id }
    var selectedGroup by remember(reqUid, defaultGroup) { mutableStateOf(defaultGroup) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("access_request_card_$reqUid"),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.displayName.ifBlank { request.email },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Google: ${request.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    if (request.spotifyEmail.isNotBlank()) {
                        Text(
                            text = "Spotify: ${request.spotifyEmail}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotifyEmerald,
                        )
                    }
                    if (request.phone.isNotBlank()) {
                        Text(
                            text = "WhatsApp: +${request.phone}",
                            style = MonoNumericSmall.copy(fontSize = 12.sp),
                            color = TextMuted,
                        )
                    }
                }
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Assign Group:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                FilterChip(
                    selected = selectedGroup == AtaPotifyConstants.GROUP_1,
                    onClick = { selectedGroup = AtaPotifyConstants.GROUP_1 },
                    label = { Text("Family 1 ($group1ActiveCount/6)") },
                    modifier = Modifier.testTag("req_group_1_$reqUid"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary,
                        selectedContainerColor = EmeraldContainer,
                        selectedLabelColor = SpotifyEmerald,
                    ),
                )
                FilterChip(
                    selected = selectedGroup == AtaPotifyConstants.GROUP_2,
                    onClick = { selectedGroup = AtaPotifyConstants.GROUP_2 },
                    label = { Text("Family 2 ($group2ActiveCount/6)") },
                    modifier = Modifier.testTag("req_group_2_$reqUid"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary,
                        selectedContainerColor = Color(0xFF0E2A38),
                        selectedLabelColor = Color(0xFF38BDF8),
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("decline_request_$reqUid"),
                    border = BorderStroke(1.dp, CoralPending.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = CoralPending,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Decline", color = CoralPending)
                }
                Button(
                    onClick = { onApprove(selectedGroup) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("approve_request_$reqUid"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Approve (${AtaPotifyConstants.familyGroupShortBadge(selectedGroup)})")
                }
            }
        }
    }
}

@Composable
private fun WhitelistRosterCard(
    member: WhitelistMember,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onCopySpotifyEmail: () -> Unit,
    onSwitchGroup: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whitelist_card_${member.memberKey}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(16.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FamilyGroupBadge(
                            familyGroup = member.normalizedGroup,
                            onClick = if (!isBusy) onSwitchGroup else null,
                        )
                        if (!member.active) {
                            Surface(color = CoralContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralPending,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "Google: ${member.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    if (member.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = SpotifyEmerald,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "+${member.phone}",
                                style = MonoNumericSmall.copy(fontSize = 12.sp),
                                color = TextMuted,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSwitchGroup,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("switch_group_${member.memberKey}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Family Group for ${member.displayName}",
                            tint = SpotifyEmerald,
                        )
                    }
                    Switch(
                        checked = member.active,
                        onCheckedChange = onToggleActive,
                        enabled = !isBusy,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = SpotifyEmerald,
                        ),
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${member.displayName}",
                            tint = TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        enabled = !isBusy,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${member.displayName}",
                            tint = CoralPending,
                        )
                    }
                }
            }

            // Reported Spotify Email Banner
            Surface(
                color = if (member.spotifyEmail.isNotBlank()) EmeraldContainer.copy(alpha = 0.55f) else AmberContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (member.spotifyEmail.isNotBlank()) SpotifyEmerald.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.3f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (member.spotifyEmail.isNotBlank()) SpotifyEmerald else AmberWarning,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (member.spotifyEmail.isNotBlank()) {
                                "Spotify: ${member.spotifyEmail}"
                            } else {
                                "Spotify email: Awaiting member login"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (member.spotifyEmail.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (member.spotifyEmail.isNotBlank()) SpotifyEmerald else AmberWarning,
                        )
                    }
                    if (member.spotifyEmail.isNotBlank()) {
                        IconButton(
                            onClick = onCopySpotifyEmail,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("copy_spotify_email_${member.memberKey}"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Spotify email for ${member.displayName}",
                                tint = SpotifyEmerald,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


