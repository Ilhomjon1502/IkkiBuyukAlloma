package uz.mnsh.buyuklar.utils

import kotlinx.coroutines.*
import java.io.IOException

fun <T> lazyDeferred(block: suspend CoroutineScope.() -> T): Lazy<Deferred<T>> {
    return lazy {
        GlobalScope.async(start = CoroutineStart.LAZY) {
            block.invoke(this)
        }
    }
}

class NoConnectivityException: IOException()