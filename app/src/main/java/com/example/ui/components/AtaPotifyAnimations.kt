package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSoftText
import com.example.ui.theme.SpotifyEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.sin

private val Family2Accent = Color(0xFF38BDF8)
private val Family2Container = Color(0xFF0E2A38)

fun Modifier.pressBounceClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press_bounce_scale",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@Composable
fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = SpotifyEmerald,
    barCount: Int = 4,
    isActive: Boolean = true,
    width: Dp = 18.dp,
    height: Dp = 16.dp,
) {
    val transition = rememberInfiniteTransition(label = "equalizer_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "equalizer_phase",
    )

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val totalBars = barCount.coerceAtLeast(3)
        val spacing = size.width * 0.14f
        val barWidth = (size.width - spacing * (totalBars - 1)) / totalBars
        for (i in 0 until totalBars) {
            val fraction = if (isActive) {
                val offset = i * 1.35f
                val wave = (sin(phase + offset) + 1f) / 2f
                0.25f + 0.75f * wave
            } else {
                0.28f
            }
            val barHeight = (size.height * fraction).coerceAtLeast(size.height * 0.2f)
            val left = i * (barWidth + spacing)
            val top = size.height - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun AnimatedStreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    if (streakCount <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streak_flame_scale",
    )

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "streak_entrance_scale",
    )

    Surface(
        color = AmberContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
        modifier = modifier
            .scale(entranceScale)
            .testTag("streak_badge_$streakCount"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AmberWarning,
                modifier = Modifier
                    .size(12.dp)
                    .scale(flameScale),
            )
            Text(
                text = if (streakCount == 1) "1-mo paid" else "${streakCount}-mo streak",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = AmberWarning,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun FamilyGroupBadge(
    familyGroup: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val normalized = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)
    val isGroup2 = normalized == AtaPotifyConstants.GROUP_2
    val fg = if (isGroup2) Family2Accent else SpotifyEmerald
    val bg = if (isGroup2) Family2Container else EmeraldContainer
    val label = AtaPotifyConstants.familyGroupShortBadge(normalized)

    val chipModifier = if (onClick != null) {
        modifier.pressBounceClickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        color = bg.copy(alpha = 0.9f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
        modifier = chipModifier.testTag("family_group_badge_$normalized"),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun FamilyGroupFilterBar(
    selectedGroup: String,
    allCount: Int,
    group1Count: Int,
    group2Count: Int,
    showSlotCapacity: Boolean = false,
    onSelectGroup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(allCount, group1Count, group2Count, showSlotCapacity) {
        val g1Cap = if (showSlotCapacity) "$group1Count/${AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS}" else "$group1Count"
        val g2Cap = if (showSlotCapacity) "$group2Count/${AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS}" else "$group2Count"
        listOf(
            Triple(AtaPotifyConstants.GROUP_ALL, "All Groups ($allCount)", SpotifyEmerald),
            Triple(AtaPotifyConstants.GROUP_1, "Family 1 ($g1Cap)", SpotifyEmerald),
            Triple(AtaPotifyConstants.GROUP_2, "Family 2 ($g2Cap)", Family2Accent),
        )
    }

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_group_filter_bar"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (groupKey, label, accentColor) ->
                val isSelected = selectedGroup == groupKey
                val bg by animateColorAsState(
                    targetValue = when {
                        !isSelected -> Color.Transparent
                        groupKey == AtaPotifyConstants.GROUP_2 -> Family2Container
                        else -> EmeraldContainer
                    },
                    animationSpec = tween(220),
                    label = "group_filter_bg_$groupKey",
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) accentColor else TextSecondary,
                    animationSpec = tween(220),
                    label = "group_filter_fg_$groupKey",
                )

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .pressBounceClickable { onSelectGroup(groupKey) }
                        .testTag("group_filter_chip_$groupKey"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (groupKey == AtaPotifyConstants.GROUP_ALL) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedAnnouncementBanner(
    announcements: List<String>,
    isAdmin: Boolean,
    onRemoveSlide: ((Int) -> Unit)? = null,
    onClearAnnouncement: (() -> Unit)? = null,
    slideDurationMillis: Int = 5_000,
    modifier: Modifier = Modifier,
) {
    val cleanSlides = remember(announcements) {
        announcements.map { it.trim() }.filter { it.isNotEmpty() }
    }
    val visible = cleanSlides.isNotEmpty()
    var currentIndex by remember(cleanSlides.size) { mutableIntStateOf(0) }
    val safeIndex = if (cleanSlides.isEmpty()) 0 else currentIndex.coerceIn(0, cleanSlides.lastIndex)
    val slideProgress = remember { Animatable(0f) }

    LaunchedEffect(cleanSlides, safeIndex) {
        if (cleanSlides.size > 1) {
            slideProgress.snapTo(0f)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = slideDurationMillis, easing = LinearEasing),
            )
            currentIndex = (safeIndex + 1) % cleanSlides.size
        } else {
            slideProgress.snapTo(1f)
        }
    }

    val infinite = rememberInfiniteTransition(label = "announcement_glow")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "megaphone_pulse",
    )

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        ) + fadeIn(tween(280)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        val currentMessage = cleanSlides.getOrNull(safeIndex).orEmpty()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (cleanSlides.size > 1) {
                        Modifier.pressBounceClickable(pressedScale = 0.985f) {
                            currentIndex = (safeIndex + 1) % cleanSlides.size
                        }
                    } else {
                        Modifier
                    }
                )
                .testTag("pinned_announcement_banner"),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF143823), SurfaceElevated, SurfaceDark),
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(SpotifyEmerald.copy(alpha = 0.16f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Pinned Announcement",
                                tint = SpotifyEmerald,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (cleanSlides.size > 1) "PINNED SLIDESHOW" else "PINNED BY ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyEmerald,
                            )
                            if (cleanSlides.size > 1) {
                                Surface(
                                    color = EmeraldContainer,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = "${safeIndex + 1}/${cleanSlides.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = SpotifyEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                                Text(
                                    text = "• tap for next",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted,
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        AnimatedContent(
                            targetState = safeIndex to currentMessage,
                            transitionSpec = {
                                (slideInVertically(
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 2 },
                                ) + fadeIn(tween(280))).togetherWith(
                                    slideOutVertically(
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                        targetOffsetY = { -it / 2 },
                                    ) + fadeOut(tween(180))
                                )
                            },
                            label = "pinned_announcement_slide_content",
                        ) { (_, slideText) ->
                            Text(
                                text = slideText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        if (cleanSlides.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                cleanSlides.indices.forEach { dotIndex ->
                                    val isCurrent = dotIndex == safeIndex
                                    val dotWidth by animateDpAsState(
                                        targetValue = if (isCurrent) 18.dp else 6.dp,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "slide_dot_$dotIndex",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(5.dp)
                                            .width(dotWidth)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrent) SpotifyEmerald else SpotifyEmerald.copy(alpha = 0.28f)
                                            )
                                            .clickable { currentIndex = dotIndex }
                                            .testTag("pinned_slide_dot_$dotIndex"),
                                    )
                                }
                            }
                        }
                    }

                    if (isAdmin && (onRemoveSlide != null || onClearAnnouncement != null)) {
                        IconButton(
                            onClick = {
                                if (onRemoveSlide != null) {
                                    onRemoveSlide(safeIndex)
                                } else {
                                    onClearAnnouncement?.invoke()
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_pinned_announcement_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove pinned announcement slide",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (cleanSlides.size > 1) {
                    LinearProgressIndicator(
                        progress = { slideProgress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = SpotifyEmerald.copy(alpha = 0.85f),
                        trackColor = SurfaceHighest.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedAnnouncementBanner(
    announcement: String,
    isAdmin: Boolean,
    onClearAnnouncement: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PinnedAnnouncementBanner(
        announcements = if (announcement.isBlank()) emptyList() else listOf(announcement),
        isAdmin = isAdmin,
        onClearAnnouncement = onClearAnnouncement,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyMixCard(
    stats: List<ListeningStat>,
    onOpenInSpotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTracks = remember(stats) {
        stats.filter { it.topTrack.isNotBlank() || it.topArtist.isNotBlank() }
    }
    if (activeTracks.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_mix_card"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SpotifyEmerald.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF123320), SurfaceDark),
                    )
                )
                .padding(16.dp),
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedEqualizerBars(
                                width = 16.dp,
                                height = 14.dp,
                                barColor = SpotifyEmerald,
                                isActive = true,
                            )
                        }
                        Column {
                            Text(
                                text = "Family Mix — On Repeat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap any member's #1 track or artist to open in Spotify",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoftText,
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activeTracks.forEach { stat ->
                        val query = buildString {
                            if (stat.topTrack.isNotBlank()) append(stat.topTrack)
                            if (stat.topArtist.isNotBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(stat.topArtist)
                            }
                        }.trim()
                        val headline = when {
                            stat.topTrack.isNotBlank() && stat.topArtist.isNotBlank() ->
                                "${stat.topTrack} • ${stat.topArtist}"
                            stat.topTrack.isNotBlank() -> stat.topTrack
                            else -> stat.topArtist
                        }

                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .pressBounceClickable(onClick = { onOpenInSpotify(query) })
                                .testTag("family_mix_chip_${stat.memberKey}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = SpotifyEmerald,
                                    modifier = Modifier.size(14.dp),
                                )
                                Column {
                                    Text(
                                        text = headline,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${stat.displayName}'s #1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in Spotify",
                                    tint = SpotifyEmerald,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
