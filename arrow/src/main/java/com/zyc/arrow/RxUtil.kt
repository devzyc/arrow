@file:Suppress("unused")

package com.zyc.arrow

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * @author zeng_yong_chang@163.com
 */
object RxUtil {

  fun <T> applyScheduler(): ObservableTransformer<T, T> {
    return ObservableTransformer { observable: Observable<T> ->
      observable.subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
    }
  }

  fun <T> applyDbSchedulerForFlowable(): FlowableTransformer<T, T> {
    return FlowableTransformer { flowable: Flowable<T> ->
      flowable.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }
  }

  fun applyDbSchedulerForCompletable(): CompletableTransformer {
    return CompletableTransformer { completable: Completable ->
      completable.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun doNothing(resp: Any?) {
  }
  
  fun disposeIfNeeded(vararg disposables: Disposable?) {
    for (disposable in disposables) {
      if (disposable != null && !disposable.isDisposed) {
        disposable.dispose()
      }
    }
  }

  fun <T> wrapNonNull(input: T?, NOT_FOUND_THING: T): T {
    return input ?: NOT_FOUND_THING
  }

  fun <T> unwrapNonNull(input: T, NOT_FOUND_THING: T): T? {
    return if (input === NOT_FOUND_THING) null else input
  }
}