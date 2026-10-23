package ir.torob.seller.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SuccessGreen,
    onSecondary = PureWhite,
    secondaryContainer = SuccessGreenLight,
    onSecondaryContainer = SuccessGreenDark,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRedDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun TorobSellerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PureWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    // Enforce Right-to-Left (RTL) LayoutDirection and Vazir typography for Persian UI
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily = VazirFontFamily)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
