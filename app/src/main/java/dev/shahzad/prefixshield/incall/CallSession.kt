package dev.shahzad.prefixshield.incall

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import dev.shahzad.prefixshield.NumberMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AudioRouteOption(
    val route: Int,
    val label: String
)

data class CallUiModel(
    val hasCall: Boolean = false,
    val number: String = "",
    val name: String = "",
    val state: Int = Call.STATE_DISCONNECTED,
    val incoming: Boolean = false,
    val muted: Boolean = false,
    val audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    val audioOptions: List<AudioRouteOption> = emptyList()
) {
    val title: String get() = name.ifBlank { number.ifBlank { "Unknown" } }

    val audioLabel: String
        get() = audioOptions.firstOrNull { it.route == audioRoute }?.label
            ?: when (audioRoute) {
                CallAudioState.ROUTE_SPEAKER -> "Speaker"
                CallAudioState.ROUTE_BLUETOOTH -> "Bluetooth"
                CallAudioState.ROUTE_WIRED_HEADSET -> "Headset"
                else -> "Earpiece"
            }

    val status: String
        get() = when (state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> "Calling…"
            Call.STATE_ACTIVE -> "Connected"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTING -> "Ending…"
            else -> ""
        }
}

object CallSession {
    private val _ui = MutableStateFlow(CallUiModel())
    val ui: StateFlow<CallUiModel> = _ui

    @Volatile
    var service: InCallService? = null
        private set

    private val tracked = LinkedHashMap<Call, Call.Callback>()
    private var userPickedRoute = false

    fun bind(incall: InCallService) {
        service = incall
    }

    fun unbind(incall: InCallService) {
        if (service === incall) service = null
    }

    fun add(call: Call) {
        if (tracked.containsKey(call)) {
            publish()
            return
        }
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state == Call.STATE_DISCONNECTED) remove(call) else publish()
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                publish()
            }
        }
        tracked[call] = callback
        call.registerCallback(callback)
        preferBluetooth()
        publish()
    }

    fun remove(call: Call) {
        tracked.remove(call)?.let { runCatching { call.unregisterCallback(it) } }
        if (tracked.isEmpty()) userPickedRoute = false
        publish()
    }

    fun answer() {
        primary()?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        primary()?.reject(false, null)
    }

    fun hangup() {
        primary()?.disconnect()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
        publish()
    }

    fun setAudioRoute(route: Int) {
        userPickedRoute = true
        service?.setAudioRoute(route)
        publish()
    }

    fun dtmf(digit: Char) {
        val call = primary() ?: return
        call.playDtmfTone(digit)
        call.stopDtmfTone()
    }

    fun publish() {
        val call = primary()
        val details = call?.details
        val audio = service?.callAudioState
        val raw = details?.handle?.schemeSpecificPart ?: details?.handle?.toString().orEmpty()
        val name = details?.callerDisplayName.orEmpty()
        if (!userPickedRoute) preferBluetooth()
        _ui.value = CallUiModel(
            hasCall = call != null,
            number = NumberMatcher.extractNumber(raw).ifBlank { raw },
            name = name,
            state = call?.state ?: Call.STATE_DISCONNECTED,
            incoming = call?.state == Call.STATE_RINGING,
            muted = audio?.isMuted == true,
            audioRoute = audio?.route ?: CallAudioState.ROUTE_EARPIECE,
            audioOptions = audioOptions(audio)
        )
    }

    private fun preferBluetooth() {
        val audio = service?.callAudioState ?: return
        if (userPickedRoute) return
        if (audio.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0 &&
            audio.route != CallAudioState.ROUTE_BLUETOOTH
        ) {
            service?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
        }
    }

    @SuppressLint("MissingPermission")
    private fun audioOptions(audio: CallAudioState?): List<AudioRouteOption> {
        if (audio == null) {
            return listOf(
                AudioRouteOption(CallAudioState.ROUTE_EARPIECE, "Earpiece"),
                AudioRouteOption(CallAudioState.ROUTE_SPEAKER, "Speaker")
            )
        }
        val mask = audio.supportedRouteMask
        val options = mutableListOf<AudioRouteOption>()
        if (mask and CallAudioState.ROUTE_EARPIECE != 0 || mask and CallAudioState.ROUTE_WIRED_OR_EARPIECE != 0) {
            options += AudioRouteOption(CallAudioState.ROUTE_EARPIECE, "Earpiece")
        }
        if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
            options += AudioRouteOption(CallAudioState.ROUTE_WIRED_HEADSET, "Headset")
        }
        if (mask and CallAudioState.ROUTE_SPEAKER != 0) {
            options += AudioRouteOption(CallAudioState.ROUTE_SPEAKER, "Speaker")
        }
        if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) {
            val btName = runCatching { audio.activeBluetoothDevice?.name }.getOrNull()
                ?.takeIf { it.isNotBlank() }
            options += AudioRouteOption(CallAudioState.ROUTE_BLUETOOTH, btName ?: "Bluetooth")
        }
        return options.distinctBy { it.route }
    }

    private fun primary(): Call? = tracked.keys.lastOrNull()
}
