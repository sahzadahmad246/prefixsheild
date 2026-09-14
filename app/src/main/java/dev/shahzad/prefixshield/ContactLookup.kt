package dev.shahzad.prefixshield

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactLookup {
    fun isSavedNumber(context: Context, rawNumbers: Collection<String?>): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val candidates = rawNumbers
            .map { NumberMatcher.extractNumber(it) }
            .filter { it.length >= 7 }
            .distinct()
        return candidates.any { lookup(context, it) }
    }

    private fun lookup(context: Context, digits: String): Boolean {
        val forms = NumberMatcher.keys(digits, isPrefix = false)
        return forms.any { form ->
            if (form.length < 7) return@any false
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(form)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor -> cursor.moveToFirst() } == true
        }
    }
}
