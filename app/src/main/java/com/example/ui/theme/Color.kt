package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF0D0F0E)
val SurfaceDark = Color(0xFF151917)
val SurfaceElevated = Color(0xFF1C221F)
val SurfaceHighest = Color(0xFF252D29)
val BorderSubtle = Color(0xFF2B3530)

val SpotifyEmerald = Color(0xFF1ED760)
val SpotifyEmeraldDeep = Color(0xFF1DB954)
val OnEmerald = Color(0xFF032310)
val EmeraldContainer = Color(0xFF113621)
val EmeraldSoftText = Color(0xFF86EFAC)

val TextPrimary = Color(0xFFF5F7F6)
val TextSecondary = Color(0xFF9BA8A1)
val TextMuted = Color(0xFF6B7771)

val AmberWarning = Color(0xFFFBBF24)
val AmberContainer = Color(0xFF33260E)

val CoralPending = Color(0xFFF87171)
val CoralContainer = Color(0xFF38171A)

fun parseGroupColorHex(hex: String?, fallback: Color = SpotifyEmerald): Color {
    val clean = hex?.trim().orEmpty()
    if (!clean.startsWith("#") || clean.length != 7) return fallback
    return runCatching {
        val rgb = clean.substring(1).toLong(16)
        Color(0xFF000000L or rgb)
    }.getOrDefault(fallback)
}
