package io.github.arfaouiahmed1.pursivo.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PursivoBlue = Color(0xFF315BA8)
val PursivoBlueDark = Color(0xFFAFC6FF)
val PursivoGreen = Color(0xFF2F765A)
val PursivoSaffron = Color(0xFFFFBA39)
val PursivoInk = Color(0xFF172033)
val PursivoMist = Color(0xFFF6F7FB)
val PursivoSuccess = Color(0xFF257A52)

private val LightColors = lightColorScheme(
    primary = PursivoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = PursivoGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F1D4),
    onSecondaryContainer = Color(0xFF002116),
    tertiary = Color(0xFF845500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDAA),
    onTertiaryContainer = Color(0xFF2A1700),
    background = PursivoMist,
    onBackground = PursivoInk,
    surface = Color(0xFFFCF8FF),
    onSurface = PursivoInk,
    surfaceVariant = Color(0xFFE2E5EF),
    onSurfaceVariant = Color(0xFF444751),
)

private val DarkColors = darkColorScheme(
    primary = PursivoBlueDark,
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF174384),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFF99D5B8),
    onSecondary = Color(0xFF003827),
    secondaryContainer = Color(0xFF14513E),
    onSecondaryContainer = Color(0xFFB4F1D4),
    tertiary = Color(0xFFFFB951),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF643F00),
    onTertiaryContainer = Color(0xFFFFDDAA),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFE2E2EA),
    surface = Color(0xFF12151D),
    onSurface = Color(0xFFE2E2EA),
    surfaceVariant = Color(0xFF444751),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

private val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex, FontWeight.Normal),
    Font(R.font.roboto_flex, FontWeight.Medium),
    Font(R.font.roboto_flex, FontWeight.SemiBold),
    Font(R.font.roboto_flex, FontWeight.Bold),
    Font(R.font.roboto_flex, FontWeight.Black),
)

private val PursivoTypography = Typography(
    displayLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 50.sp),
    headlineLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
)

private val PursivoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(topStart = 36.dp, topEnd = 18.dp, bottomEnd = 36.dp, bottomStart = 18.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PursivoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = PursivoTypography,
        shapes = PursivoShapes,
        content = content,
    )
}
