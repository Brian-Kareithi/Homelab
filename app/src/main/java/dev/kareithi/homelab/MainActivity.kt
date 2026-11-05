package dev.kareithi.homelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.data.settings.ThemeMode
import dev.kareithi.homelab.ui.HomelabRoot
import dev.kareithi.homelab.ui.theme.HomelabTheme
import dev.kareithi.homelab.ui.theme.Slate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = (application as HomelabApp).container.settings

        setContent {
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings
            if (s == null) {
                // DataStore not read yet: keep the slate window background, no theme flash.
                Box(Modifier.fillMaxSize().background(Slate.S900))
                return@setContent
            }
            val dark = when (s.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LaunchedEffect(dark) {
                val transparent = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent)
                    else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent)
                    else SystemBarStyle.light(transparent, transparent),
                )
            }
            HomelabTheme(
                themeMode = s.theme,
                accent = Color(s.accent),
                dynamicColor = s.dynamicColor,
            ) {
                HomelabRoot()
            }
        }
    }
}
