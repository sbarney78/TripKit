package au.barney.tripkit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMaroonDark,
    onPrimary = OnPrimaryMaroonDark,
    secondary = SecondaryLightTeal,
    onSecondary = Color.Black,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMaroon,
    onPrimary = OnPrimaryMaroon,
    secondary = SecondaryTeal,
    onSecondary = OnSecondaryTeal,
    primaryContainer = PrimaryLightMaroon,
    onPrimaryContainer = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    error = ErrorRed
)

@Composable
fun TripKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
