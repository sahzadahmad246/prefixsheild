package dev.shahzad.prefixshield.screening

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import dev.shahzad.prefixshield.BlockRecorder
import dev.shahzad.prefixshield.BlockSettings
import dev.shahzad.prefixshield.BlockedCall
import dev.shahzad.prefixshield.CallDecision
import dev.shahzad.prefixshield.NumberMatcher

class PrefixScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
                respondToCall(callDetails, allow())
                return
            }

            if (!BlockSettings(this).isBlockingEnabled()) {
                respondToCall(callDetails, allow())
                return
            }

            val raws = numberCandidates(callDetails)
            addContactHandle(callDetails, raws)
            val matched = CallDecision.shouldBlock(this, raws)

            if (matched == null) {
                respondToCall(callDetails, allow())
                return
            }

            BlockRecorder.record(
                this,
                BlockedCall(
                    number = NumberMatcher.displayNumber(raws),
                    matchedPrefix = matched,
                    atMillis = System.currentTimeMillis()
                )
            )

            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false)
                    .build()
            )
        } catch (error: Exception) {
            Log.e(TAG, "Screening failed; allowing call", error)
            respondToCall(callDetails, allow())
        }
    }

    private fun numberCandidates(details: Call.Details): MutableList<String> {
        val raws = mutableListOf<String>()
        fun add(value: String?) {
            if (!value.isNullOrBlank()) raws += value
        }
        add(details.handle?.toString())
        add(details.handle?.schemeSpecificPart)
        add(details.gatewayInfo?.originalAddress?.toString())
        add(details.gatewayInfo?.originalAddress?.schemeSpecificPart)
        collectFromBundle(details.extras, raws)
        collectFromBundle(details.intentExtras, raws)
        return raws
    }

    private fun addContactHandle(details: Call.Details, raws: MutableList<String>) {
        if (Build.VERSION.SDK_INT >= 30) {
            details.contactDisplayName?.toString()?.let { if (it.isNotBlank()) raws += it }
        }
        details.callerDisplayName?.let { if (it.isNotBlank()) raws += it }
    }

    @Suppress("DEPRECATION")
    private fun collectFromBundle(bundle: android.os.Bundle?, raws: MutableList<String>) {
        if (bundle == null) return
        for (key in bundle.keySet()) {
            when (val value = bundle.get(key)) {
                is String -> if (value.isNotBlank()) raws += value
                is android.net.Uri -> {
                    val part = value.schemeSpecificPart
                    if (!part.isNullOrBlank()) raws += part
                    raws += value.toString()
                }
            }
        }
    }

    private fun allow(): CallResponse = CallResponse.Builder().build()

    companion object {
        private const val TAG = "PrefixShield"
    }
}
