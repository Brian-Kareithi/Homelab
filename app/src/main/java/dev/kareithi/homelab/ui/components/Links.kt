package dev.kareithi.homelab.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import dev.kareithi.homelab.ui.theme.HomelabTheme

/** Opens [url] in a Custom Tab, falling back to any browser. */
fun openUrl(context: Context, url: String, toolbarColor: Int) {
    val fixed = if (url.contains("://")) url else "http://$url"
    val uri = fixed.toUri()
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build(),
            )
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No browser available to open $fixed", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Remembers a lambda that opens URLs themed with the current card colour. */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    val color = HomelabTheme.colors.card.toArgb()
    return { url -> openUrl(context, url, color) }
}
