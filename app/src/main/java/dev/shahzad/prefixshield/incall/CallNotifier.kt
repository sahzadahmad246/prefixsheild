package dev.shahzad.prefixshield.incall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import dev.shahzad.prefixshield.R

object CallNotifier {
    const val CHANNEL_ID = "phone_calls"
    const val NOTIFICATION_ID = 71

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun show(service: InCallService, model: CallUiModel) {
        createChannel(service)
        val notification = build(service, model) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                service.startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                service.startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                androidx.core.app.NotificationManagerCompat.from(service).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
            }
        }
    }

    fun cancel(context: Context) {
        (context as? InCallService)?.stopForeground(InCallService.STOP_FOREGROUND_REMOVE)
        androidx.core.app.NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun build(context: Context, model: CallUiModel): Notification? {
        if (!model.hasCall) return null
        val person = Person.Builder().setName(model.title).setImportant(true).build()
        val open = activityIntent(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(model.title)
            .setContentText(if (model.incoming && model.state == Call.STATE_RINGING) "mobile" else model.status)
            .setSubText("myPhone")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (model.incoming && model.state == Call.STATE_RINGING) {
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    broadcast(context, CallActionReceiver.REJECT),
                    broadcast(context, CallActionReceiver.ANSWER)
                )
            ).setFullScreenIntent(open, true)
        } else {
            builder.setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    broadcast(context, CallActionReceiver.HANGUP)
                )
            )
            if (model.connectedAt > 0L) {
                builder.setUsesChronometer(true).setWhen(model.connectedAt).setShowWhen(true)
            }
            val holdLabel = if (model.holding) "Resume" else "Hold"
            builder.addAction(
                android.R.drawable.ic_media_pause,
                holdLabel,
                broadcast(context, CallActionReceiver.HOLD)
            )
        }
        return builder.build()
    }

    private fun activityIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, InCallActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcast(context: Context, action: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
