package dev.shahzad.prefixshield.incall

object AppForeground {
    @Volatile
    private var resumed = 0

    val isVisible: Boolean get() = resumed > 0

    fun onResume() {
        resumed++
    }

    fun onPause() {
        resumed = (resumed - 1).coerceAtLeast(0)
    }
}
