package dev.shahzad.prefixshield.incall

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import dev.shahzad.prefixshield.BlockRecorder
import dev.shahzad.prefixshield.BlockSettings
import dev.shahzad.prefixshield.BlockedCall
import dev.shahzad.prefixshield.CallDecision
import dev.shahzad.prefixshield.NumberMatcher

class PrefixInCallService : InCallService() {
    override fun onBind(intent: Intent): android.os.IBinder? {
        val binder = super.onBind(intent)
        CallSession.bind(this)
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        CallSession.unbind(this)
        IncomingCallNotifier.cancel(this)
        return super.onUnbind(intent)
    }

    override fun onCallAdded(call: Call) {
        val details = call.details
        val raws = listOfNotNull(
            details?.handle?.toString(),
            details?.handle?.schemeSpecificPart
        )
        if (call.state == Call.STATE_RINGING && BlockSettings(this).isBlockingEnabled()) {
            val matched = CallDecision.shouldBlock(this, raws)
            if (matched != null) {
                BlockRecorder.record(
                    this,
                    BlockedCall(
                        number = NumberMatcher.displayNumber(raws),
                        matchedPrefix = matched,
                        atMillis = System.currentTimeMillis()
                    )
                )
                call.reject(false, null)
                return
            }
        }
        CallSession.add(call)
        val title = details?.callerDisplayName
            ?: details?.handle?.schemeSpecificPart
            ?: "Call"
        IncomingCallNotifier.show(this, title, call.state == Call.STATE_RINGING)
        startActivity(
            Intent(this, InCallActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        )
    }

    override fun onCallRemoved(call: Call) {
        CallSession.remove(call)
        if (!CallSession.ui.value.hasCall) IncomingCallNotifier.cancel(this)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallSession.publish()
    }
}
