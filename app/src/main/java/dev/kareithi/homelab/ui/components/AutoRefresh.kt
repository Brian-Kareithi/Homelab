package dev.kareithi.homelab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Refreshes once when the screen becomes visible, then every [intervalSeconds] (0 = off) while
 * the lifecycle is at least STARTED. Stops automatically in the background.
 */
@Composable
fun AutoRefreshEffect(intervalSeconds: Int, onRefresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onRefresh)
    LaunchedEffect(owner, intervalSeconds) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            latest()
            if (intervalSeconds > 0) {
                while (true) {
                    delay(intervalSeconds * 1_000L)
                    latest()
                }
            }
        }
    }
}
