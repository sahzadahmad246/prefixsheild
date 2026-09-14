package dev.shahzad.prefixshield.screening

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dev.shahzad.prefixshield.BlockRecorder
import dev.shahzad.prefixshield.BlockSettings
import dev.shahzad.prefixshield.BlockedCall
import dev.shahzad.prefixshield.CallDecision
import dev.shahzad.prefixshield.NumberMatcher

class IncomingCallReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        if (!BlockSettings(context).isBlockingEnabled()) return

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?: intent.getStringExtra("incoming_number")
        val matched = CallDecision.shouldBlock(context, listOf(number))
            ?: return

        BlockRecorder.record(
            context,
            BlockedCall(
                number = NumberMatcher.extractNumber(number).ifBlank { number ?: "Unknown" },
                matchedPrefix = matched,
                atMillis = System.currentTimeMillis()
            )
        )

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telecom.endCall()
        }
    }
}
