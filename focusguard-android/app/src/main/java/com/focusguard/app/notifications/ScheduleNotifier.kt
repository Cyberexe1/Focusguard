package com.focusguard.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.focusguard.app.MainActivity
import com.focusguard.app.R

object ScheduleNotifier {

    const val CHANNEL_ID = "focusguard_schedule"
    private const val CHANNEL_NAME = "Schedule Reminders"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    /** Create the notification channel (safe to call multiple times). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifies you when a scheduled focus block begins."
                    enableVibration(true)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Schedule a notification to fire at [triggerAtMillis].
     * Falls back to an inexact alarm if exact alarms aren't permitted.
     */
    fun scheduleAt(
        context: Context,
        notifId: Int,
        title: String,
        message: String,
        triggerAtMillis: Long,
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    /** Cancel a previously scheduled notification. */
    fun cancel(context: Context, notifId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    /** Show the notification immediately (called by the receiver). */
    fun show(context: Context, notifId: Int, title: String, message: String) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(notifId, notification)
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ScheduleNotifier.EXTRA_TITLE) ?: "Schedule reminder"
        val message = intent.getStringExtra(ScheduleNotifier.EXTRA_MESSAGE) ?: "Your scheduled block is starting now."
        val notifId = intent.getIntExtra(ScheduleNotifier.EXTRA_NOTIF_ID, 0)
        ScheduleNotifier.show(context, notifId, title, message)
    }
}

// ── Daily check-in notification (9 AM every day) ─────────────────────────────

object CheckInNotifier {
    private const val NOTIF_ID = 9001
    private const val CHANNEL_ID = "focusguard_checkin"
    private const val REQ_CODE = 9001

    fun schedule(context: Context) {
        ensureChannel(context)

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            // If 9 AM already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, CheckInAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Daily Check-in", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds you to check in on your tasks every morning."
                    enableVibration(true)
                })
            }
        }
    }

    fun show(context: Context) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context, NOTIF_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("☑️ Daily Check-in")
            .setContentText("Mark today's progress on your tasks. You've got this!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mark today's progress on your tasks. Staying consistent builds momentum. You've got this!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, notification)

        // Reschedule for next day
        schedule(context)
    }
}

class CheckInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CheckInNotifier.show(context)
    }
}
