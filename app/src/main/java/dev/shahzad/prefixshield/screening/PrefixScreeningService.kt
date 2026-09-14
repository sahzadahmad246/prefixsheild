package dev.shahzad.prefixshield.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import dev.shahzad.prefixshield.BlockedCall
import dev.shahzad.prefixshield.NumberMatcher
import dev.shahzad.prefixshield.PrefixStore
import dev.shahzad.prefixshield.BlockedLogStore

class PrefixScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
                respondToCall(callDetails, allow())
                return
            }

            val number = numberFrom(callDetails)
            val rules = PrefixStore(this).list()
            val matched = NumberMatcher.matchingPrefix(number, rules)

            if (matched == null) {
                respondToCall(callDetails, allow())
                return
            }

            try {
                BlockedLogStore(this).add(
                    BlockedCall(
                        number = number.ifBlank { "Unknown" },
                        matchedPrefix = matched,
                        atMillis = System.currentTimeMillis()
                    )
                )
            } catch (error: Exception) {
                Log.w(TAG, "Could not save blocked call", error)
            }

            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSilenceCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false)
                    .build()
            )
        } catch (error: Exception) {
            Log.e(TAG, "Screening failed; allowing call", error)
            respondToCall(callDetails, allow())
        }
    }

    private fun numberFrom(details: Call.Details): String {
        val handle = details.handle?.schemeSpecificPart
        val gateway = details.gatewayInfo?.originalAddress?.schemeSpecificPart
        return NumberMatcher.extractNumber(handle ?: gateway)
    }

    private fun allow(): CallResponse = CallResponse.Builder().build()

    companion object {
        private const val TAG = "PrefixShield"
    }
}
