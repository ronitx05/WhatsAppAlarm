package com.example.whatsappalarm

import android.app.Notification
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppNotificationListener : NotificationListenerService() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Only care about WhatsApp or WhatsApp Business
        if (packageName != WHATSAPP_PACKAGE && packageName != WHATSAPP_BUSINESS_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return  // sender name for 1:1, group name for groups
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        val prefs = WatchedContactsPrefs(applicationContext)
        val watchedContacts = prefs.getContacts()

        if (watchedContacts.isEmpty()) return

        val matched = watchedContacts.any { contactName ->
            // Individual chat: title is the contact name
            title.equals(contactName, ignoreCase = true) ||
                    // Group chat: text is usually "ContactName: message"
                    text.startsWith("$contactName:", ignoreCase = true)
        }

        if (matched) {
            triggerAlarm()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op
    }

    private fun triggerAlarm() {
        // Vibrate
        vibrate()

        // Play loud alarm sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                prepare()
                start()

                // Auto-stop after 10 seconds so it doesn't blare forever
                setOnCompletionListener { release() }
            }

            // Safety: stop after 10s regardless
            android.os.Handler(mainLooper).postDelayed({
                mediaPlayer?.release()
                mediaPlayer = null
            }, 10_000)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500) // wait, vibrate, pause, repeat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}