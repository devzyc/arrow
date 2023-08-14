package com.zyc.arrow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Cancellable

object RxBroadcastUtil {
    /**
     * copy from <a>https://github.com/yongjhih/rx-receiver/blob/master/rx2-receiver/src/main/java/rx2/receiver/android/RxReceiver.java</a>
     */
    fun receiveOn(
        intentFilter: IntentFilter,
        context: Context
    ): Observable<Intent?> {
        return Observable.create(ObservableOnSubscribe { emitter: ObservableEmitter<Intent?> ->
            val receiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent
                ) {
                    emitter.onNext(intent)
                }
            }
            context.registerReceiver(receiver, intentFilter)
            emitter.setCancellable(Cancellable {
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    context.unregisterReceiver(receiver)
                } else {
                    val inner = AndroidSchedulers.mainThread().createWorker()
                    inner.schedule(Runnable {
                        context.unregisterReceiver(receiver)
                        inner.dispose()
                    })
                }
            })
        })
    }
}