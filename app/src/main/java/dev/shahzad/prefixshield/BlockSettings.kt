package dev.shahzad.prefixshield

class BlockSettings(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    fun isBlockingEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).commit()
    }

    fun blockSavedNumbers(): Boolean = prefs.getBoolean(KEY_SAVED, true)

    fun setBlockSavedNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVED, enabled).commit()
    }

    companion object {
        private const val KEY_ENABLED = "blocking_enabled"
        private const val KEY_SAVED = "block_saved_numbers"
    }
}
