package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.FAMILY_GROUP_COLOR_PRESETS
import com.example.data.model.FamilyGroup
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.PlanConfig
import com.example.data.model.WhitelistMember
import com.example.ui.components.pressBounceClickable
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSoftText
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseGroupColorHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun FamilyGroupsScreen(
    isAdmin: Boolean,
    currentMemberKey: String,
    currentUserEmail: String,
    currentUserSpotifyEmail: String,
    assignedGroupId: String,
    familyGroups: List<FamilyGroup>,
    whitelist: List<WhitelistMember>,
    currentMonth: PaymentMonth?,
    currentMonthMembers: List<MemberPaymentStatus>,
    planConfig: PlanConfig,
    defaultAmountLkr: Long,
    isBusy: Boolean,
    onCopyText: (label: String, text: String) -> Unit,
    onOpenUrl: (url: String) -> Unit,
    onCreateOrUpdateGroup: (
        groupId: String?,
        name: String,
        startDate: String,
        amountLkr: Long,
        colorHex: String,
        maxSlots: Long,
        alsoUpdateActiveMonthMembers: Boolean,
        address: String,
        managerName: String,
        managerEmail: String,
        inviteLink: String,
        notes: String,
        onDone: () -> Unit,
    ) -> Unit,
    onDeleteGroup: (group: FamilyGroup, reassignToGroupId: String?, onDone: () -> Unit) -> Unit,
    onTransferMember: (
        memberKey: String,
        memberName: String,
        targetGroupId: String,
        updateActiveMonthAmount: Boolean,
        onDone: () -> Unit,
    ) -> Unit,
    onAddNewMemberToGroup: (
        email: String,
        displayName: String,
        phone: String,
        spotifyEmail: String,
        targetGroupId: String,
        onDone: () -> Unit,
    ) -> Unit,
) {
    if (!isAdmin) {
        MemberFamilyGroupView(
            assignedGroupId = assignedGroupId,
            familyGroups = familyGroups,
            currentMemberKey = currentMemberKey,
            currentUserEmail = currentUserEmail,
            currentUserSpotifyEmail = currentUserSpotifyEmail,
            currentMonth = currentMonth,
            currentMonthMembers = currentMonthMembers,
            planConfig = planConfig,
            onCopyText = onCopyText,
            onOpenUrl = onOpenUrl,
        )
        return
    }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var deletingGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var assigningToGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var transferringMember by remember { mutableStateOf<WhitelistMember?>(null) }

    val membersByGroup = remember(whitelist, familyGroups) {
        val grouped = whitelist.groupBy { it.normalizedGroup }
        familyGroups.associate { grp ->
            grp.effectiveId to grouped[grp.effectiveId].orEmpty()
        }
    }

    val totalActiveMembers = remember(whitelist) { whitelist.count { it.active } }
    val totalMonthlySplitLkr = remember(familyGroups, membersByGroup) {
        familyGroups.sumOf { grp ->
            val activeInGrp = membersByGroup[grp.effectiveId].orEmpty().count { it.active }
            activeInGrp * grp.amountLkr
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
            item(key = "groups_header_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("family_groups_overview_card"),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF143521), SurfaceDark),
                                )
                            )
                            .padding(18.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = SpotifyEmerald,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Family Groups",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "${familyGroups.size} group(s) • $totalActiveMembers active friends • Est. LKR $totalMonthlySplitLkr/mo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showCreateGroupDialog = true },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("create_family_group_button"),
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
                                Text("Create New Family Group", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            items(familyGroups, key = { it.effectiveId }) { group ->
                val groupMembers = membersByGroup[group.effectiveId].orEmpty()
                FamilyGroupManagementCard(
                    group = group,
                    members = groupMembers,
                    canDelete = familyGroups.size > 1,
                    isBusy = isBusy,
                    modifier = Modifier.animateItem(),
                    onEditGroup = { editingGroup = group },
                    onDeleteGroup = { deletingGroup = group },
                    onAssignMembers = { assigningToGroup = group },
                    onTransferMember = { member -> transferringMember = member },
                    onCopyText = onCopyText,
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCreateGroupDialog) {
        val nextPreset = remember(familyGroups.size) {
            FAMILY_GROUP_COLOR_PRESETS[familyGroups.size % FAMILY_GROUP_COLOR_PRESETS.size].hex
        }
        FamilyGroupEditorDialog(
            editingGroup = null,
            defaultAmountLkr = defaultAmountLkr,
            defaultColorHex = nextPreset,
            currentMonth = currentMonth,
            isBusy = isBusy,
            onSave = { name, startDate, amountLkr, colorHex, maxSlots, updateMonth, address, managerName, managerEmail, inviteLink, notes ->
                onCreateOrUpdateGroup(
                    null,
                    name,
                    startDate,
                    amountLkr,
                    colorHex,
                    maxSlots,
                    updateMonth,
                    address,
                    managerName,
                    managerEmail,
                    inviteLink,
                    notes,
                ) {
                    showCreateGroupDialog = false
                }
            },
            onDismiss = { showCreateGroupDialog = false },
        )
    }

    editingGroup?.let { target ->
        FamilyGroupEditorDialog(
            editingGroup = target,
            defaultAmountLkr = defaultAmountLkr,
            defaultColorHex = target.colorHex,
            currentMonth = currentMonth,
            isBusy = isBusy,
            onSave = { name, startDate, amountLkr, colorHex, maxSlots, updateMonth, address, managerName, managerEmail, inviteLink, notes ->
                onCreateOrUpdateGroup(
                    target.effectiveId,
                    name,
                    startDate,
                    amountLkr,
                    colorHex,
                    maxSlots,
                    updateMonth,
                    address,
                    managerName,
                    managerEmail,
                    inviteLink,
                    notes,
                ) {
                    editingGroup = null
                }
            },
            onDismiss = { editingGroup = null },
        )
    }

    deletingGroup?.let { target ->
        val assignedMembers = membersByGroup[target.effectiveId].orEmpty()
        val otherGroups = familyGroups.filter { it.effectiveId != target.effectiveId }
        DeleteFamilyGroupDialog(
            group = target,
            assignedCount = assignedMembers.size,
            otherGroups = otherGroups,
            isBusy = isBusy,
            onConfirmDelete = { reassignToGroupId ->
                onDeleteGroup(target, reassignToGroupId) {
                    deletingGroup = null
                }
            },
            onDismiss = { deletingGroup = null },
        )
    }

    assigningToGroup?.let { targetGroup ->
        AssignMemberToGroupDialog(
            targetGroup = targetGroup,
            allWhitelist = whitelist,
            allGroups = familyGroups,
            isBusy = isBusy,
            onTransferExisting = { member, updateMonthAmount ->
                onTransferMember(
                    member.memberKey,
                    member.displayName,
                    targetGroup.effectiveId,
                    updateMonthAmount,
                ) {
                    assigningToGroup = null
                }
            },
            onAddNewMember = { email, name, phone, spotifyEmail ->
                onAddNewMemberToGroup(
                    email,
                    name,
                    phone,
                    spotifyEmail,
                    targetGroup.effectiveId,
                ) {
                    assigningToGroup = null
                }
            },
            onDismiss = { assigningToGroup = null },
        )
    }

    transferringMember?.let { member ->
        TransferMemberGroupDialog(
            member = member,
            groups = familyGroups,
            currentMonth = currentMonth,
            isBusy = isBusy,
            onConfirmTransfer = { targetGroupId, updateMonthAmount ->
                onTransferMember(
                    member.memberKey,
                    member.displayName,
                    targetGroupId,
                    updateMonthAmount,
                ) {
                    transferringMember = null
                }
            },
            onDismiss = { transferringMember = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamilyGroupManagementCard(
    group: FamilyGroup,
    members: List<WhitelistMember>,
    canDelete: Boolean,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAssignMembers: () -> Unit,
    onTransferMember: (WhitelistMember) -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
) {
    val groupColor = parseGroupColorHex(group.colorHex, SpotifyEmerald)
    val animatedAccent by animateColorAsState(
        targetValue = groupColor,
        animationSpec = tween(320),
        label = "grp_accent_${group.effectiveId}",
    )
    val activeCount = members.count { it.active }
    val maxSlots = group.maxSlots.coerceAtLeast(1L)
    val targetFraction = (activeCount.toFloat() / maxSlots.toFloat()).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "grp_fill_${group.effectiveId}",
    )
    val startDateFormatted = remember(group.startDate) {
        AtaPotifyConstants.formatStartDateDisplay(group.startDate)
    }
    val renewalHint = remember(group.startDate) {
        AtaPotifyConstants.billingDaySummary(group.startDate)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_group_card_${group.effectiveId}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, animatedAccent.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Colored header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                animatedAccent.copy(alpha = 0.22f),
                                SurfaceElevated,
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(animatedAccent)
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                        )
                        Column {
                            Text(
                                text = group.name.ifBlank { AtaPotifyConstants.familyGroupLabel(group.effectiveId) },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (renewalHint.isNotBlank()) renewalHint else "Custom Spotify Family Plan",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEditGroup,
                            enabled = !isBusy,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_group_${group.effectiveId}"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${group.name}",
                                tint = animatedAccent,
                            )
                        }
                        if (canDelete) {
                            IconButton(
                                onClick = onDeleteGroup,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_group_${group.effectiveId}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete ${group.name}",
                                    tint = CoralPending,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Metadata pills row: Start Date + Price Split + Capacity
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .pressBounceClickable(enabled = !isBusy, onClick = onEditGroup)
                            .testTag("group_start_date_pill_${group.effectiveId}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = animatedAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Start: $startDateFormatted",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Surface(
                        color = animatedAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .pressBounceClickable(enabled = !isBusy, onClick = onEditGroup)
                            .testTag("group_price_pill_${group.effectiveId}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = animatedAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "LKR ${group.amountLkr} / friend",
                                style = MonoNumericSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = animatedAccent,
                            )
                        }
                    }

                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.pressBounceClickable(enabled = !isBusy, onClick = onEditGroup),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = animatedAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = group.colorHex.uppercase(Locale.US),
                                style = MonoNumericSmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                            )
                        }
                    }
                }

                // Capacity bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isOver = activeCount > maxSlots
                        Text(
                            text = "Slots: $activeCount / $maxSlots active" +
                                if (members.size > activeCount) " (${members.size - activeCount} paused)" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOver) CoralPending else TextSecondary,
                        )
                        Text(
                            text = "Group total: LKR ${activeCount * group.amountLkr}/mo",
                            style = MonoNumericSmall,
                            color = animatedAccent,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedFill },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (activeCount > maxSlots) CoralPending else animatedAccent,
                        trackColor = SurfaceHighest,
                    )
                }

                // Member-visible Spotify Family Info preview
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_group_spotify_info_${group.effectiveId}"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Spotify Plan Info (Visible to Members)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = animatedAccent,
                            )
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(enabled = !isBusy, onClick = onEditGroup)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }

                        if (!group.hasMemberVisibleInfo) {
                            Text(
                                text = "No Spotify Home Address, Family Manager, or Invite Link set yet. Tap Edit to add details for assigned members.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        } else {
                            if (group.managerName.isNotBlank() || group.managerEmail.isNotBlank()) {
                                val mgrText = buildString {
                                    if (group.managerName.isNotBlank()) append(group.managerName)
                                    if (group.managerEmail.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(group.managerEmail)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = animatedAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Manager: $mgrText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (group.address.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = animatedAccent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = group.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCopyText("Home Address", group.address) },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.cd_copy_text),
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            if (group.inviteLink.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = animatedAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = group.inviteLink,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSoftText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (group.notes.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = group.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Members header + Assign/Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Members (${members.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FilledTonalButton(
                        onClick = onAssignMembers,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("assign_to_group_${group.effectiveId}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = animatedAccent.copy(alpha = 0.16f),
                            contentColor = animatedAccent,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Add / Move In")
                    }
                }

                if (members.isEmpty()) {
                    Surface(
                        color = SurfaceElevated.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "No members in ${group.name} yet. Tap Add / Move In to add a new friend or transfer someone here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_member_row_${group.effectiveId}_${member.memberKey}"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (!member.active) {
                                                Surface(
                                                    color = CoralContainer,
                                                    shape = RoundedCornerShape(6.dp),
                                                ) {
                                                    Text(
                                                        text = "Paused",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = CoralPending,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (member.spotifyEmail.isNotBlank()) {
                                                "Spotify: ${member.spotifyEmail}"
                                            } else {
                                                member.email
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onTransferMember(member) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("transfer_member_${member.memberKey}"),
                                        border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.45f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Transfer ${member.displayName}",
                                            tint = animatedAccent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Move", style = MaterialTheme.typography.labelMedium, color = animatedAccent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyGroupEditorDialog(
    editingGroup: FamilyGroup?,
    defaultAmountLkr: Long,
    defaultColorHex: String,
    currentMonth: PaymentMonth?,
    isBusy: Boolean,
    onSave: (
        name: String,
        startDate: String,
        amountLkr: Long,
        colorHex: String,
        maxSlots: Long,
        updateActiveMonthMembers: Boolean,
        address: String,
        managerName: String,
        managerEmail: String,
        inviteLink: String,
        notes: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(editingGroup) { mutableStateOf(editingGroup?.name.orEmpty()) }
    var startDate by remember(editingGroup) {
        mutableStateOf(
            editingGroup?.startDate?.takeIf { it.isNotBlank() }
                ?: AtaPotifyConstants.todayIsoDate()
        )
    }
    var amountText by remember(editingGroup, defaultAmountLkr) {
        mutableStateOf((editingGroup?.amountLkr ?: defaultAmountLkr).toString())
    }
    var colorHex by remember(editingGroup, defaultColorHex) {
        mutableStateOf(AtaPotifyConstants.normalizeHexColor(editingGroup?.colorHex ?: defaultColorHex))
    }
    var maxSlotsText by remember(editingGroup) {
        mutableStateOf((editingGroup?.maxSlots ?: AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong()).toString())
    }
    var address by remember(editingGroup) { mutableStateOf(editingGroup?.address.orEmpty()) }
    var managerName by remember(editingGroup) { mutableStateOf(editingGroup?.managerName.orEmpty()) }
    var managerEmail by remember(editingGroup) { mutableStateOf(editingGroup?.managerEmail.orEmpty()) }
    var inviteLink by remember(editingGroup) { mutableStateOf(editingGroup?.inviteLink.orEmpty()) }
    var notes by remember(editingGroup) { mutableStateOf(editingGroup?.notes.orEmpty()) }
    var updateActiveMonth by remember(editingGroup) { mutableStateOf(editingGroup != null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val previewColor = parseGroupColorHex(colorHex, SpotifyEmerald)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                )
                Text(if (editingGroup == null) "Create Family Group" else "Edit ${editingGroup.name}")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Family Group Name (e.g. Family 3)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_editor_name_input"),
                )

                // Starting Date picker row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Plan Starting Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { input ->
                                startDate = input.filter { it.isDigit() || it == '-' }.take(10)
                            },
                            label = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("group_editor_start_date_input"),
                        )
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("group_editor_pick_date_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = previewColor.copy(alpha = 0.16f),
                                contentColor = previewColor,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Open date picker",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Pick")
                        }
                    }
                    val formattedPreview = AtaPotifyConstants.formatStartDateDisplay(startDate)
                    val renewalPreview = AtaPotifyConstants.billingDaySummary(startDate)
                    if (renewalPreview.isNotBlank()) {
                        Text(
                            text = "$formattedPreview • $renewalPreview",
                            style = MaterialTheme.typography.labelSmall,
                            color = previewColor,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Split (LKR)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("group_editor_amount_input"),
                    )
                    OutlinedTextField(
                        value = maxSlotsText,
                        onValueChange = { maxSlotsText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Max Slots") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("group_editor_slots_input"),
                    )
                }

                // Group Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Group Theme Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FAMILY_GROUP_COLOR_PRESETS.forEach { preset ->
                            val swatchColor = parseGroupColorHex(preset.hex, SpotifyEmerald)
                            val isSelected = colorHex.equals(preset.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else BorderSubtle,
                                        shape = CircleShape,
                                    )
                                    .clickable { colorHex = preset.hex }
                                    .testTag("color_swatch_${preset.hex.removePrefix("#")}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected ${preset.name}",
                                        tint = ObsidianBg,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { raw ->
                            val cleaned = raw.uppercase(Locale.US)
                                .filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }
                                .take(7)
                            colorHex = if (cleaned.startsWith("#")) cleaned else "#${cleaned.take(6)}"
                        },
                        label = { Text("Custom Hex Color (#RRGGBB)") },
                        singleLine = true,
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(previewColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_hex_input"),
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Member-Visible Spotify Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = previewColor,
                    )
                    Text(
                        text = "Only members assigned to this group will see these details on their Groups tab.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it.take(80) },
                        label = { Text("Spotify Family Manager Name") },
                        placeholder = { Text("e.g. Thathsara") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_manager_name_input"),
                    )
                    OutlinedTextField(
                        value = managerEmail,
                        onValueChange = { managerEmail = it.take(160) },
                        label = { Text("Manager Spotify Email / Contact (optional)") },
                        placeholder = { Text("e.g. owner@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_manager_email_input"),
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it.take(240) },
                        label = { Text("Spotify Home Address") },
                        placeholder = { Text("Exact street address for Spotify verification") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_address_input"),
                    )
                    OutlinedTextField(
                        value = inviteLink,
                        onValueChange = { inviteLink = it.take(300) },
                        label = { Text("Spotify Family Invite Link (optional)") },
                        placeholder = { Text("https://www.spotify.com/family/join/...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_invite_link_input"),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(320) },
                        label = { Text("Notes / Join Instructions (optional)") },
                        placeholder = { Text("e.g. Use mobile browser & enter address above") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_editor_notes_input"),
                    )
                }

                if (editingGroup != null && currentMonth != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Update LKR split for this group in ${currentMonth.displayLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = updateActiveMonth,
                            onCheckedChange = { updateActiveMonth = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = previewColor,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            val slots = (maxSlotsText.toLongOrNull() ?: 6L).coerceIn(1L, 20L)
            val validDate = startDate.isBlank() || startDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
            val validHex = AtaPotifyConstants.isValidHexColor(colorHex)
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        startDate.trim(),
                        amount,
                        colorHex.uppercase(Locale.US),
                        slots,
                        updateActiveMonth,
                        address.trim(),
                        managerName.trim(),
                        managerEmail.trim(),
                        inviteLink.trim(),
                        notes.trim(),
                    )
                },
                enabled = !isBusy && name.isNotBlank() && amount in 1..100_000 && validDate && validHex,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("group_editor_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = previewColor,
                    contentColor = ObsidianBg,
                ),
            ) {
                Text(if (editingGroup == null) "Create Group" else "Save Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )

    if (showDatePicker) {
        val initialMillis = remember(startDate) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fmt.parse(startDate)?.time
            }.getOrNull() ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            startDate = fmt.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = SpotifyEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeleteFamilyGroupDialog(
    group: FamilyGroup,
    assignedCount: Int,
    otherGroups: List<FamilyGroup>,
    isBusy: Boolean,
    onConfirmDelete: (reassignToGroupId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reassignTarget by remember(group, otherGroups) {
        mutableStateOf(otherGroups.firstOrNull()?.effectiveId)
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
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = CoralPending,
                )
                Text("Delete ${group.name}?")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (assignedCount > 0) {
                        "${group.name} currently has $assignedCount member(s). Choose which group should receive them before deleting:"
                    } else {
                        "Are you sure you want to delete ${group.name}?"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (assignedCount > 0 && otherGroups.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        otherGroups.forEach { option ->
                            val optColor = parseGroupColorHex(option.colorHex, SpotifyEmerald)
                            val selected = reassignTarget == option.effectiveId
                            Surface(
                                color = if (selected) optColor.copy(alpha = 0.2f) else SurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) optColor else BorderSubtle,
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressBounceClickable { reassignTarget = option.effectiveId }
                                    .testTag("reassign_target_${option.effectiveId}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(optColor),
                                    )
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) optColor else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(if (assignedCount > 0) reassignTarget else null) },
                enabled = !isBusy && (assignedCount == 0 || reassignTarget != null),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_delete_group_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPending,
                    contentColor = ObsidianBg,
                ),
            ) {
                Text("Delete Group")
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
private fun TransferMemberGroupDialog(
    member: WhitelistMember,
    groups: List<FamilyGroup>,
    currentMonth: PaymentMonth?,
    isBusy: Boolean,
    onConfirmTransfer: (targetGroupId: String, updateActiveMonthAmount: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val destinationGroups = remember(groups, member) {
        groups.filter { it.effectiveId != member.normalizedGroup }
    }
    var selectedTarget by remember(member, destinationGroups) {
        mutableStateOf(destinationGroups.firstOrNull()?.effectiveId ?: member.normalizedGroup)
    }
    var syncMonthAmount by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Transfer ${member.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select the destination Family Group:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (destinationGroups.isEmpty()) {
                    Text(
                        text = "Create another Family Group first to transfer members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        destinationGroups.forEach { grp ->
                            val grpColor = parseGroupColorHex(grp.colorHex, SpotifyEmerald)
                            val selected = selectedTarget == grp.effectiveId
                            Surface(
                                color = if (selected) grpColor.copy(alpha = 0.16f) else SurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) grpColor else BorderSubtle,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressBounceClickable { selectedTarget = grp.effectiveId }
                                    .testTag("transfer_dest_${grp.effectiveId}"),
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
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(grpColor),
                                        )
                                        Text(
                                            text = grp.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) grpColor else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = "LKR ${grp.amountLkr}",
                                        style = MonoNumericSmall,
                                        color = grpColor,
                                    )
                                }
                            }
                        }
                    }

                    if (currentMonth != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Update ${currentMonth.displayLabel} amount to new group rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = syncMonthAmount,
                                onCheckedChange = { syncMonthAmount = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = SpotifyEmerald,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmTransfer(selectedTarget, syncMonthAmount) },
                enabled = !isBusy && destinationGroups.isNotEmpty() && selectedTarget != member.normalizedGroup,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_transfer_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyEmerald,
                    contentColor = ObsidianBg,
                ),
            ) {
                Text("Transfer")
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
private fun AssignMemberToGroupDialog(
    targetGroup: FamilyGroup,
    allWhitelist: List<WhitelistMember>,
    allGroups: List<FamilyGroup>,
    isBusy: Boolean,
    onTransferExisting: (member: WhitelistMember, updateMonthAmount: Boolean) -> Unit,
    onAddNewMember: (email: String, displayName: String, phone: String, spotifyEmail: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val targetColor = parseGroupColorHex(targetGroup.colorHex, SpotifyEmerald)
    val candidatesFromOtherGroups = remember(allWhitelist, targetGroup) {
        allWhitelist.filter { it.normalizedGroup != targetGroup.effectiveId }
    }
    var modeAddNew by remember(candidatesFromOtherGroups) {
        mutableStateOf(candidatesFromOtherGroups.isEmpty())
    }
    var selectedExisting by remember(candidatesFromOtherGroups) {
        mutableStateOf(candidatesFromOtherGroups.firstOrNull())
    }
    var syncAmount by remember { mutableStateOf(true) }

    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newSpotifyEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(targetColor),
                )
                Text("Add to ${targetGroup.name}")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (candidatesFromOtherGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { modeAddNew = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (!modeAddNew) targetColor.copy(alpha = 0.2f) else SurfaceElevated,
                                contentColor = if (!modeAddNew) targetColor else TextSecondary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Move Existing")
                        }
                        FilledTonalButton(
                            onClick = { modeAddNew = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (modeAddNew) targetColor.copy(alpha = 0.2f) else SurfaceElevated,
                                contentColor = if (modeAddNew) targetColor else TextSecondary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Add New")
                        }
                    }
                }

                if (!modeAddNew && candidatesFromOtherGroups.isNotEmpty()) {
                    Text(
                        text = "Pick a friend from another group to transfer into ${targetGroup.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidatesFromOtherGroups.forEach { candidate ->
                            val selected = selectedExisting?.memberKey == candidate.memberKey
                            val currentGrpLabel = AtaPotifyConstants.familyGroupLabel(candidate.normalizedGroup, allGroups)
                            Surface(
                                color = if (selected) targetColor.copy(alpha = 0.16f) else SurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) targetColor else BorderSubtle,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressBounceClickable { selectedExisting = candidate }
                                    .testTag("move_candidate_${candidate.memberKey}"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = candidate.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Currently in $currentGrpLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = targetColor,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Apply LKR ${targetGroup.amountLkr} split to active month",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = syncAmount,
                            onCheckedChange = { syncAmount = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = targetColor,
                            ),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_add_member_name_input"),
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Google Account Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_add_member_email_input"),
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                        label = { Text("WhatsApp Number (e.g. 94771234567)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_add_member_phone_input"),
                    )
                    OutlinedTextField(
                        value = newSpotifyEmail,
                        onValueChange = { newSpotifyEmail = it.trim() },
                        label = { Text("Spotify Account Email (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_add_member_spotify_input"),
                    )
                }
            }
        },
        confirmButton = {
            if (!modeAddNew && candidatesFromOtherGroups.isNotEmpty()) {
                Button(
                    onClick = {
                        selectedExisting?.let { onTransferExisting(it, syncAmount) }
                    },
                    enabled = !isBusy && selectedExisting != null,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("group_move_confirm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = targetColor,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Text("Move Here")
                }
            } else {
                Button(
                    onClick = { onAddNewMember(newEmail, newName, newPhone, newSpotifyEmail) },
                    enabled = !isBusy && newName.isNotBlank() && newEmail.contains("@"),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("group_add_member_confirm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = targetColor,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Text("Add to ${targetGroup.name}")
                }
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
private fun MemberFamilyGroupView(
    assignedGroupId: String,
    familyGroups: List<FamilyGroup>,
    currentMemberKey: String,
    currentUserEmail: String,
    currentUserSpotifyEmail: String,
    currentMonth: PaymentMonth?,
    currentMonthMembers: List<MemberPaymentStatus>,
    planConfig: PlanConfig,
    onCopyText: (label: String, text: String) -> Unit,
    onOpenUrl: (url: String) -> Unit,
) {
    val normalizedAssigned = remember(assignedGroupId) {
        AtaPotifyConstants.normalizeFamilyGroup(assignedGroupId)
    }
    val group = remember(familyGroups, normalizedAssigned) {
        familyGroups.firstOrNull { it.effectiveId == normalizedAssigned } ?: familyGroups.firstOrNull()
    }

    if (group == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Your Family Group info hasn't been configured yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    val groupColor = parseGroupColorHex(group.colorHex, SpotifyEmerald)
    val animatedAccent by animateColorAsState(
        targetValue = groupColor,
        animationSpec = tween(320),
        label = "member_grp_accent",
    )
    val startDateFormatted = remember(group.startDate) {
        AtaPotifyConstants.formatStartDateDisplay(group.startDate)
    }
    val renewalHint = remember(group.startDate) {
        AtaPotifyConstants.billingDaySummary(group.startDate)
    }
    val selfMonthMember = remember(currentMonthMembers, currentMemberKey) {
        currentMonthMembers.firstOrNull { it.memberKey == currentMemberKey }
    }
    val groupPeers = remember(currentMonthMembers, group.effectiveId) {
        currentMonthMembers.filter { it.normalizedGroup == group.effectiveId }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("member_family_group_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Assigned Group Hero Card
            item(key = "member_group_hero") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_group_hero_card"),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, animatedAccent.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        animatedAccent.copy(alpha = 0.24f),
                                        SurfaceDark,
                                    )
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
                                Surface(
                                    color = animatedAccent.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.45f)),
                                ) {
                                    Text(
                                        text = stringResource(R.string.title_my_family_group),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = animatedAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                if (groupPeers.isNotEmpty()) {
                                    Text(
                                        text = "${groupPeers.size} / ${group.maxSlots} slots active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(animatedAccent)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name.ifBlank { AtaPotifyConstants.familyGroupLabel(group.effectiveId) },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (renewalHint.isNotBlank()) {
                                            "Started $startDateFormatted • $renewalHint"
                                        } else {
                                            "Started $startDateFormatted"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    color = animatedAccent.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.4f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = animatedAccent,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "LKR ${group.amountLkr} / month",
                                            style = MonoNumericSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = animatedAccent,
                                        )
                                    }
                                }

                                val acctLabel = currentUserSpotifyEmail.ifBlank { currentUserEmail }
                                if (acctLabel.isNotBlank()) {
                                    Surface(
                                        color = SurfaceElevated,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                    ) {
                                        Text(
                                            text = if (currentUserSpotifyEmail.isNotBlank()) {
                                                "Spotify: $currentUserSpotifyEmail"
                                            } else {
                                                "Account: $currentUserEmail"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Spotify Setup Details Card (Address, Family Manager, Invite Link, Notes)
            item(key = "member_group_spotify_details") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_group_info_card"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Spotify Family Setup Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        // Home Address Block
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = animatedAccent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.label_spotify_address),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    if (group.address.isNotBlank()) {
                                        FilledTonalButton(
                                            onClick = { onCopyText("Spotify Home Address", group.address) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("member_copy_address_button"),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = animatedAccent.copy(alpha = 0.16f),
                                                contentColor = animatedAccent,
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text("Copy", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                if (group.address.isNotBlank()) {
                                    Text(
                                        text = group.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Use this exact address when accepting your Spotify Family invite or verifying your location.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                } else {
                                    Text(
                                        text = "Not set by Admin yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                        }

                        // Spotify Family Manager Block
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = animatedAccent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.label_family_manager),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                if (group.managerName.isNotBlank() || group.managerEmail.isNotBlank()) {
                                    if (group.managerName.isNotBlank()) {
                                        Text(
                                            text = group.managerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    if (group.managerEmail.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = group.managerEmail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { onCopyText("Manager Contact", group.managerEmail) },
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = stringResource(R.string.cd_copy_text),
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Not specified yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                        }

                        // Invite Link Block (if provided)
                        if (group.inviteLink.isNotBlank()) {
                            Surface(
                                color = EmeraldContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = animatedAccent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.label_invite_link),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = group.inviteLink,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldSoftText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val isWebUrl = group.inviteLink.startsWith("https://", ignoreCase = true) ||
                                            group.inviteLink.startsWith("http://", ignoreCase = true)
                                        if (isWebUrl) {
                                            Button(
                                                onClick = { onOpenUrl(group.inviteLink) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("member_open_invite_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = animatedAccent,
                                                    contentColor = ObsidianBg,
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Open Invite", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onCopyText("Invite Link", group.inviteLink) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("member_copy_invite_button"),
                                            border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = animatedAccent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Copy Link", color = animatedAccent)
                                        }
                                    }
                                }
                            }
                        }

                        // Notes & Join Instructions Block (if provided)
                        if (group.notes.isNotBlank()) {
                            Surface(
                                color = AmberContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = stringResource(R.string.label_group_notes),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberWarning,
                                        )
                                        Text(
                                            text = group.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Payment Status & Bank Transfer Card
            if (selfMonthMember != null || planConfig.hasBankDetails) {
                item(key = "member_group_payment_info") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (currentMonth != null && selfMonthMember != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = currentMonth.displayLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = when {
                                                selfMonthMember.status == AtaPotifyConstants.STATUS_PAID ->
                                                    if (selfMonthMember.isCashPayment) "Settled via Cash in Hand" else "Paid & Verified"
                                                selfMonthMember.hasPartialPayment ->
                                                    "Paid LKR ${selfMonthMember.effectivePaidLkr} • LKR ${selfMonthMember.remainingLkr} left"
                                                selfMonthMember.status == AtaPotifyConstants.STATUS_VERIFYING ->
                                                    "Receipt uploaded • Awaiting Admin confirmation"
                                                else -> "LKR ${selfMonthMember.amountLkr} due"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    val statusColor = when (selfMonthMember.status) {
                                        AtaPotifyConstants.STATUS_PAID -> SpotifyEmerald
                                        AtaPotifyConstants.STATUS_VERIFYING -> AmberWarning
                                        else -> CoralPending
                                    }
                                    val statusLabel = when {
                                        selfMonthMember.status == AtaPotifyConstants.STATUS_PAID -> "Paid"
                                        selfMonthMember.status == AtaPotifyConstants.STATUS_VERIFYING -> "Verifying"
                                        selfMonthMember.hasPartialPayment -> "Partial"
                                        else -> "Pending"
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.16f),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }

                            if (planConfig.hasBankDetails) {
                                if (selfMonthMember != null) {
                                    HorizontalDivider(color = BorderSubtle)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = SpotifyEmerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.title_bank_details),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onCopyText("Bank Details", planConfig.formattedBankCopyText())
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EmeraldContainer,
                                            contentColor = SpotifyEmerald,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Text(
                                    text = planConfig.formattedBankCopyText(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Plan Peers in This Group (Display names only — zero PII)
            if (groupPeers.isNotEmpty()) {
                item(key = "member_group_peers") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Members in ${group.name} (${groupPeers.size}/${group.maxSlots})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                groupPeers.forEach { peer ->
                                    val isSelf = peer.memberKey == currentMemberKey
                                    Surface(
                                        color = if (isSelf) animatedAccent.copy(alpha = 0.16f) else SurfaceElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelf) animatedAccent.copy(alpha = 0.5f) else BorderSubtle,
                                        ),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (peer.status == AtaPotifyConstants.STATUS_PAID) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SpotifyEmerald,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                            Text(
                                                text = if (isSelf) "${peer.displayName} (You)" else peer.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelf) animatedAccent else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

