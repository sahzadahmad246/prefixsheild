package dev.shahzad.prefixshield.incall

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object CallRingtone {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    fun start(context: Context) {
        if (ringtone?.isPlaying == true) return
        val app = context.applicationContext
        val audio = app.getSystemService(AudioManager::class.java)
        val mode = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            @Suppress("DEPRECATION")
            audio?.requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            val uri = RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val tone = runCatching { RingtoneManager.getRingtone(app, uri) }.getOrNull()
            if (tone != null) {
                tone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .build()
                if (Build.VERSION.SDK_INT >= 28) tone.isLooping = true
                runCatching { tone.play() }
                ringtone = tone
            }
        }
        if (mode != AudioManager.RINGER_MODE_SILENT) startVibrate(app)
    }

    fun stop() {
        runCatching { ringtone?.stop() }
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun startVibrate(context: Context) {
        val vibe = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        vibrator = vibe
        val pattern = longArrayOf(0, 500, 500, 500)
        if (Build.VERSION.SDK_INT >= 26) {
            vibe.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(pattern, 0)
        }
    }
}
