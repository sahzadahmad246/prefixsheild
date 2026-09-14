package dev.shahzad.prefixshield

import android.content.Context

object CallDecision {
    fun shouldBlock(context: Context, rawNumbers: Collection<String?>): String? {
        val settings = BlockSettings(context)
        if (!settings.isBlockingEnabled()) return null
        val matched = NumberMatcher.matchingPrefix(rawNumbers, PrefixStore(context).list())
            ?: return null
        val saved = ContactLookup.isSavedNumber(context, rawNumbers)
        if (saved && !settings.blockSavedNumbers()) return null
        return matched
    }
}
