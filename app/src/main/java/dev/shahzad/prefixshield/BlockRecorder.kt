package dev.shahzad.prefixshield

import android.content.Context

object BlockRecorder {
    fun record(context: Context, call: BlockedCall) {
        BlockedLogStore(context).add(call)
        val pending = PendingNotifications(context)
        pending.add(call.number)
        BlockedNotifier.showUnseen(context, pending.list())
    }
}
