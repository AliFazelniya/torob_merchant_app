package ir.torob.seller.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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
    primary = TorobRed,
    onPrimary = PureWhite,
    primaryContainer = TorobLightRed,
    onPrimaryContainer = TorobDarkRed,
    secondary = TorobGreen,
    onSecondary = PureWhite,
    background = Slate50,
    surface = PureWhite,
    onBackground = Slate900,
    onSurface = Slate900
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
            window.statusBarColor = TorobRed.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Force Right-to-Left (RTL) LayoutDirection and Vazir font for Persian user interface
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
