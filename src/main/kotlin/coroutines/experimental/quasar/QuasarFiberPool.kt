package coroutines.experimental.quasar

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.FiberScheduler
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

val fiberSchedulerName = "Fiber Scheduler CoroutineContext"

fun fiberScheduler(parrallel: Int) = Pool(FiberForkJoinScheduler(fiberSchedulerName, 100))

class Pool(val scheduler: FiberScheduler) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            PoolContinuation(scheduler, continuation.context.fold(continuation, { cont, element ->
                if (element != this@Pool && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }))
}

private class PoolContinuation<T>(
        val scheduler: FiberScheduler,
        val continuation: Continuation<T>
) : Continuation<T> by continuation {
    override fun resume(value: T) {
        Fiber<Unit>(scheduler) {
            continuation.resume(value)
        }.start()
    }

    override fun resumeWithException(exception: Throwable) {
        Fiber<Unit>(scheduler) {
            continuation.resumeWithException(exception)
        }.start()
    }
}