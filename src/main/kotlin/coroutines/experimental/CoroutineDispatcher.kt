
package coroutines.experimental

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Base class that shall be extended by all coroutine dispatcher implementations.
 *
 * The following standard implementations are provided by `kotlinx.coroutines`:
 * * [Unconfined] -- starts coroutine execution in the current call-frame until the first suspension.
 *   On first  suspension the coroutine builder function returns.
 *   The coroutine will resume in whatever thread that is used by the
 *   corresponding suspending function, without confining it to any specific thread or pool.
 *   This in an appropriate choice for IO-intensive coroutines that do not consume CPU resources.
 * * [DefaultDispatcher] -- is used by all standard builder if no dispatcher nor any other [ContinuationInterceptor]
 *   is specified in their context. It is currently equal to [CommonPool] (subject to change).
 * * [CommonPool] -- immediately returns from the coroutine builder and schedules coroutine execution to
 *   a common pool of shared background threads.
 *   This is an appropriate choice for compute-intensive coroutines that consume a lot of CPU resources.
 * * Private thread pools can be created with [newSingleThreadContext] and [newFixedThreadPoolContext].
 * * An arbitrary [Executor][java.util.concurrent.Executor] can be converted to dispatcher with [asCoroutineDispatcher] extension function.
 *
 * This class ensures that debugging facilities in [newCoroutineContext] function work properly.
 */
abstract class CoroutineDispatcher(
    parallel: Int
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

        private val DISPATCHED_CONTINUATION_UPDATER = AtomicReferenceFieldUpdater.newUpdater(CoroutineDispatcher::class.java,
                DispatchedContinuation::class.java, "dispatchedContinuation")
    @Volatile private var dispatchedContinuation: DispatchedContinuation<*>? = null
    private val INDEX_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CoroutineDispatcher::class.java, "index")
    @Volatile private var index: Int = 0

    /**
     * Returns `true` if execution shall be dispatched onto another thread.
     * The default behaviour for most dispatchers is to return `true`.
     *
     * UI dispatchers _should not_ override `isDispatchNeeded`, but leave a default implementation that
     * returns `true`. To understand the rationale beyond this recommendation, consider the following code:
     *
     * ```kotlin
     * fun asyncUpdateUI() = async(MainThread) {
     *     // do something here that updates something in UI
     * }
     * ```
     *
     * When you invoke `asyncUpdateUI` in some background thread, it immediately continues to the next
     * line, while UI update happens asynchronously in the UI thread. However, if you invoke
     * it in the UI thread itself, it updates UI _synchronously_ if your `isDispatchNeeded` is
     * overridden with a thread check. Checking if we are already in the UI thread seems more
     * efficient (and it might indeed save a few CPU cycles), but this subtle and context-sensitive
     * difference in behavior makes the resulting async code harder to debug.
     *
     * Basically, the choice here is between "JS-style" asynchronous approach (async actions
     * are always postponed to be executed later in the even dispatch thread) and "C#-style" approach
     * (async actions are executed in the invoker thread until the first suspension point).
     * While, C# approach seems to be more efficient, it ends up with recommendations like
     * "use `yield` if you need to ....". This is error-prone. JS-style approach is more consistent
     * and does not require programmers to think about whether they need to yield or not.
     *
     * However, coroutine builders like [launch] and [async] accept an optional [CoroutineStart]
     * parameter that allows one to optionally choose C#-style [CoroutineStart.UNDISPATCHED] behaviour
     * whenever it is needed for efficiency.
     */
    public open fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    /**
     * Dispatches execution of a runnable [block] onto another thread in the given [context].
     */
    public abstract fun dispatch(context: CoroutineContext, block: Runnable)

    /**
     * Returns continuation that wraps the original [continuation], thus intercepting all resumptions.
     */
    public override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val index = INDEX_UPDATER.getAndIncrement(this)
        val dispatchedContinuation = DISPATCHED_CONTINUATION_UPDATER.getAndUpdate(this) {
            (it as DispatchedContinuation<T>).addContinuation(continuation)
            return@getAndUpdate it
        }
        return dispatchedContinuation as DispatchedContinuation<T>
//        DispatchedContinuation(this, continuation)
    }

    // for nicer debugging
    override fun toString(): String =
        "${this::class.java.simpleName}@${Integer.toHexString(System.identityHashCode(this))}"
}
