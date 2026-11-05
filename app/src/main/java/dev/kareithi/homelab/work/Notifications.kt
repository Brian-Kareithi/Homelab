package dev.kareithi.homelab.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.kareithi.homelab.MainActivity
import dev.kareithi.homelab.R
import dev.kareithi.homelab.data.settings.NotificationKind

/** One channel per category, so each can be silenced separately in system settings. */
object Notifications {

    fun channelId(kind: NotificationKind) = "homelab_${kind.key}"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = NotificationKind.entries.map { kind ->
            val (name, desc, importance) = when (kind) {
                NotificationKind.DOCKER -> Triple(R.string.channel_docker, R.string.channel_docker_desc, NotificationManager.IMPORTANCE_HIGH)
                NotificationKind.SERVER -> Triple(R.string.channel_server, R.string.channel_server_desc, NotificationManager.IMPORTANCE_HIGH)
                NotificationKind.TORRENTS -> Triple(R.string.channel_torrents, R.string.channel_torrents_desc, NotificationManager.IMPORTANCE_DEFAULT)
                NotificationKind.REQUESTS -> Triple(R.string.channel_requests, R.string.channel_requests_desc, NotificationManager.IMPORTANCE_DEFAULT)
            }
            NotificationChannel(channelId(kind), context.getString(name), importance).apply {
                description = context.getString(desc)
            }
        }
        nm.createNotificationChannels(channels)
    }

    fun canPost(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // checked by canPost()
    fun post(context: Context, kind: NotificationKind, tag: String, title: String, text: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channelId(kind))
            .setSmallIcon(R.drawable.ic_stat_homelab)
            .setColor(0xFF06B6D4.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        NotificationManagerCompat.from(context).notify(tag, kind.ordinal, notification)
    }
}
