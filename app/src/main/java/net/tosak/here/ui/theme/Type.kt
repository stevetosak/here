package net.tosak.here.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import net.tosak.here.R

val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

val JetBrainsMono = FontFamily(
    Font(
        googleFont   = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight       = FontWeight.Normal,
    ),
    Font(
        googleFont   = GoogleFont("JetBrains Mono"),
        fontProvider = googleFontProvider,
        weight       = FontWeight.Medium,
    ),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 10.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 28.sp,
        lineHeight    = 34.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = JetBrainsMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.18.sp,
    ),
)