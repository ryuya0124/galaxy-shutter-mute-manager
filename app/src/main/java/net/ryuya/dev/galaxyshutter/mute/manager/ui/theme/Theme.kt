package net.ryuya.dev.galaxyshutter.mute.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Manager アプリは紫系のアクセントカラーで Shutter Mute と差別化する
private val ManagerPurple = Color(0xFF7B1FA2)
private val ManagerDarkPurple = Color(0xFF4A148C)
private val ManagerLightPurple = Color(0xFFE1BEE7)

private val DarkColorScheme = darkColorScheme(
    primary = ManagerLightPurple,
    onPrimary = Color(0xFF2C0050),
    primaryContainer = ManagerDarkPurple,
    onPrimaryContainer = ManagerLightPurple,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
)

private val LightColorScheme = lightColorScheme(
    primary = ManagerPurple,
    onPrimary = Color.White,
    primaryContainer = ManagerLightPurple,
    onPrimaryContainer = ManagerDarkPurple,
    background = Color(0xFFF8F5FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EBF8),
)

/**
 * Galaxy Shutter Mute Manager アプリのテーマ
 */
@Composable
fun ManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
