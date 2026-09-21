package dev.shahzad.prefixshield.incall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ANSWER -> {
                CallRingtone.stop()
                CallSession.answer()
                openInCall(context)
            }
            REJECT -> {
                CallRingtone.stop()
                CallSession.reject()
            }
            HANGUP -> CallSession.hangup()
            HOLD -> CallSession.toggleHold()
        }
    }

    private fun openInCall(context: Context) {
        context.startActivity(
            Intent(context, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    companion object {
        const val ANSWER = "dev.shahzad.prefixshield.action.ANSWER"
        const val REJECT = "dev.shahzad.prefixshield.action.REJECT"
        const val HANGUP = "dev.shahzad.prefixshield.action.HANGUP"
        const val HOLD = "dev.shahzad.prefixshield.action.HOLD"
    }
}
