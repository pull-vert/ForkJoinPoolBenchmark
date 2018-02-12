package coroutines.experimental.simple

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.RecursiveAction
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

object CommonPool : Pool(ForkJoinPool.commonPool())

/**
 * "The" Interceptor Element for this Context
 */
open class Pool(val pool: ForkJoinPool) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            PoolContinuationRecursive(pool, continuation.context.fold(continuation, { cont, element ->
                if (element != this@Pool && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }))
}

private class PoolContinuationRecursive<T>(
        val forkJoinPool: ForkJoinPool,
        val continuation: Continuation<T>
) : RecursiveAction(), Continuation<T> by continuation {

    override fun resume(value: T) {
        if (isPoolThread()) continuation.resume(value)
        else forkJoinPool.execute { continuation.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
        if (isPoolThread()) continuation.resumeWithException(exception)
        else forkJoinPool.execute { continuation.resumeWithException(exception) }
    }

    override fun compute() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun isPoolThread(): Boolean = (Thread.currentThread() as? ForkJoinWorkerThread)?.pool == forkJoinPool
}