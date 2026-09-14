package dev.shahzad.prefixshield

import android.app.Application

class PrefixShieldApp : Application() {
    lateinit var prefixStore: PrefixStore
        private set
    lateinit var blockedLogStore: BlockedLogStore
        private set

    override fun onCreate() {
        super.onCreate()
        prefixStore = PrefixStore(this)
        blockedLogStore = BlockedLogStore(this)
        BlockedNotifier.createChannel(this)
        instance = this
    }

    companion object {
        lateinit var instance: PrefixShieldApp
            private set
    }
}
