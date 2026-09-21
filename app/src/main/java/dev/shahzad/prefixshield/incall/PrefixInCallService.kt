package dev.shahzad.prefixshield.incall

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import dev.shahzad.prefixshield.BlockRecorder
import dev.shahzad.prefixshield.BlockSettings
import dev.shahzad.prefixshield.BlockedCall
import dev.shahzad.prefixshield.CallDecision
import dev.shahzad.prefixshield.NumberMatcher

class PrefixInCallService : InCallService() {
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF && CallSession.ui.value.incoming) {
                CallRingtone.silence()
            }
        }
    }
    private var receiverRegistered = false

    override fun onBind(intent: Intent): android.os.IBinder? {
        val binder = super.onBind(intent)
        CallSession.bind(this)
        CallSession.onChanged = { refreshChrome() }
        if (!receiverRegistered) {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            receiverRegistered = true
        }
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        CallSession.onChanged = null
        CallSession.unbind(this)
        CallRingtone.stop()
        CallNotifier.cancel(this)
        if (receiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            receiverRegistered = false
        }
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
        if (call.state == Call.STATE_RINGING) {
            CallRingtone.resetSilence()
        }
        CallSession.add(call)
        launchCallUi()
    }

    override fun onCallRemoved(call: Call) {
        CallSession.remove(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallSession.publish()
    }

    private fun refreshChrome() {
        val model = CallSession.ui.value
        if (!model.hasCall) {
            CallRingtone.stop()
            CallRingtone.resetSilence()
            CallNotifier.cancel(this)
            return
        }
        CallNotifier.show(this, model)
        if (model.incoming && model.state == Call.STATE_RINGING && !CallRingtone.isSilenced()) {
            CallRingtone.start(this)
        } else {
            CallRingtone.stop()
        }
    }

    private fun launchCallUi() {
        val model = CallSession.ui.value
        val ringing = model.incoming && model.state == Call.STATE_RINGING
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        if (ringing && !locked && !AppForeground.isVisible) {
            startActivity(
                Intent(this, IncomingBannerActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            )
        } else {
            openFullCall()
        }
    }

    private fun openFullCall() {
        startActivity(
            Intent(this, InCallActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        )
    }
}
