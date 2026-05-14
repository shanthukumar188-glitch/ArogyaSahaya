package com.arogya.sahaya.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arogya.sahaya.ArogyaApplication
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.Pill
import com.arogya.sahaya.ui.MainActivity
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> AlarmScheduler.rescheduleAll(context)
            ACTION_PILL_ALARM -> {
                val name = intent.getStringExtra(EXTRA_PILL_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
                val doseTime = intent.getStringExtra(EXTRA_DOSE_TIME) ?: ""
                showNotification(context, name, dosage, doseTime)
            }
        }
    }

    private fun showNotification(ctx: Context, name: String, dosage: String, doseTime: String) {
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "pills")
        }
        val pi = PendingIntent.getActivity(ctx, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val emoji = when (doseTime) { "MORNING" -> "🌅"; "AFTERNOON" -> "☀️"; else -> "🌙" }
        val notif = NotificationCompat.Builder(ctx, ArogyaApplication.CHANNEL_PILLS)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("$emoji Time for $name")
            .setContentText("$dosage — $doseTime dose")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time to take your $name ($dosage).\nTap to open the app."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    companion object {
        const val ACTION_PILL_ALARM = "com.arogya.sahaya.PILL_ALARM"
        const val EXTRA_PILL_NAME = "pill_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_DOSE_TIME = "dose_time"
        const val EXTRA_PILL_ID = "pill_id"
    }
}

object AlarmScheduler {
    fun schedulePillAlarms(ctx: Context, pill: Pill) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (pill.doseTimesMorning) schedule(ctx, am, pill, "MORNING", pill.reminderHourMorning, pill.reminderMinMorning)
        if (pill.doseTimesAfternoon) schedule(ctx, am, pill, "AFTERNOON", pill.reminderHourAfternoon, pill.reminderMinAfternoon)
        if (pill.doseTimesNight) schedule(ctx, am, pill, "NIGHT", pill.reminderHourNight, pill.reminderMinNight)
    }

    private fun schedule(ctx: Context, am: AlarmManager, pill: Pill, doseTime: String, hour: Int, min: Int) {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PILL_ALARM
            putExtra(AlarmReceiver.EXTRA_PILL_NAME, pill.name)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, pill.dosage)
            putExtra(AlarmReceiver.EXTRA_DOSE_TIME, doseTime)
            putExtra(AlarmReceiver.EXTRA_PILL_ID, pill.id)
        }
        val rc = "${pill.id}_$doseTime".hashCode()
        val pi = PendingIntent.getBroadcast(ctx, rc, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        } catch (e: SecurityException) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        }
    }

    fun cancelPillAlarms(ctx: Context, pill: Pill) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf("MORNING", "AFTERNOON", "NIGHT").forEach { doseTime ->
            val pi = PendingIntent.getBroadcast(ctx, "${pill.id}_$doseTime".hashCode(),
                Intent(ctx, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi)
        }
    }

    fun rescheduleAll(ctx: Context) { /* Called on BOOT — inject DB in production */ }
}
