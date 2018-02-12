//package coroutines.experimental
//
//import kotlinx.coroutines.experimental.CompletedExceptionally
//import kotlinx.coroutines.experimental.CoroutineDispatcher
//import kotlinx.coroutines.experimental.CoroutineName
//import kotlinx.coroutines.experimental.Job
//import kotlinx.coroutines.experimental.Runnable
//import kotlinx.coroutines.experimental.internal.Symbol
//import java.util.concurrent.RecursiveAction
//import java.util.concurrent.RunnableFuture
//import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
//import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
//import kotlin.coroutines.experimental.AbstractCoroutineContextElement
//import kotlin.coroutines.experimental.Continuation
//import kotlin.coroutines.experimental.CoroutineContext
//
//class ForkJoinPoolDispatcher(
//    parallel: Int
//): CoroutineDispatcher() {
//    private val DISPATCHED_CONTINUATION_UPDATER = AtomicReferenceFieldUpdater.newUpdater(ForkJoinPoolDispatcher::class.java,
//            ForkJoinTaskDispatchedContinuation::class.java, "dispatchedContinuation")
//    @Volatile private var dispatchedContinuation: ForkJoinTaskDispatchedContinuation<*>? = null
//    private val INDEX_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ForkJoinPoolDispatcher::class.java, "index")
//    @Volatile private var index: Int = 0
//    override fun dispatch(context: CoroutineContext, block: Runnable) {
//
//    }
//
//    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
//        val index = INDEX_UPDATER.getAndIncrement(this)
//        dispatchedContinuation.addContinuation(continuation)
//        ForkJoinTaskDispatchedContinuation(this, continuation)
//    }
//
//}
//
//@Suppress("PrivatePropertyName")
//private val UNDEFINED = Symbol("UNDEFINED")
//
//class ForkJoinTaskDispatchedContinuation<T>(
//        val forkJoinPoolDispatcher: ForkJoinPoolDispatcher,
//        val continuation: Continuation<T>
//) : RecursiveAction(), RunnableFuture<Void>, Continuation<T> by continuation {
//    private val continuations = arrayListOf<Continuation<T>>()
//    init {
//        continuations.add(continuation)
//    }
//
//    private var _state: Any? = UNDEFINED
//    var resumeMode: Int = 0
//
//    override fun run() {
//        invoke()
//    }
//
//    fun addContinuation(continuation: Continuation<T>) {
//        continuations.add(continuation)
//    }
//
//    fun takeState(): Any? {
//        val state = _state
//        check(state !== UNDEFINED) // fail-fast if repeatedly invoked
//        _state = UNDEFINED
//        return state
//    }
//
//    val delegate: Continuation<T>
//        get() = this
//
//    @Suppress("UNCHECKED_CAST")
//    fun <T> getSuccessfulResult(state: Any?): T =
//            state as T
//
//    fun getExceptionalResult(state: Any?): Throwable? =
//            (state as? CompletedExceptionally)?.exception
//
//    public override fun compute() {
//        try {
//            val delegate = delegate as ForkJoinTaskDispatchedContinuation<T>
//            val continuation = delegate.continuation
//            val context = continuation.context
//            val job = if (resumeMode.isCancellableMode) context[Job] else null
//            val state = takeState() // NOTE: Must take state in any case, even if cancelled
//            withCoroutineContext(context) {
//                if (job != null && !job.isActive)
//                    continuation.resumeWithException(job.getCancellationException())
//                else {
//                    val exception = getExceptionalResult(state)
//                    if (exception != null)
//                        continuation.resumeWithException(exception)
//                    else
//                        continuation.resume(getSuccessfulResult(state))
//                }
//            }
//        } catch (e: Throwable) {
//            throw DispatchException("Unexpected exception running $this", e)
//        }
//    }
//}
//
//@PublishedApi internal const val MODE_ATOMIC_DEFAULT = 0 // schedule non-cancellable dispatch for suspendCoroutine
//@PublishedApi internal const val MODE_CANCELLABLE = 1    // schedule cancellable dispatch for suspendCancellableCoroutine
//@PublishedApi internal const val MODE_DIRECT = 2         // when the context is right just invoke the delegate continuation direct
//@PublishedApi internal const val MODE_UNDISPATCHED = 3   // when the thread is right, but need to mark it with current coroutine
//@PublishedApi internal const val MODE_IGNORE = 4         // don't do anything
//
//internal val Int.isCancellableMode get() = this == MODE_CANCELLABLE
//internal val Int.isDispatchedMode get() = this == MODE_ATOMIC_DEFAULT || this == MODE_CANCELLABLE
//
//internal class DispatchException actual constructor(message: String, cause: Throwable) : RuntimeException(message, cause)
//
//private const val DEBUG_PROPERTY_NAME = "kotlinx.coroutines.debug"
//
//private val DEBUG = run {
//    val value = try { System.getProperty(DEBUG_PROPERTY_NAME) }
//    catch (e: SecurityException) { null }
//    when (value) {
//        "auto", null -> CoroutineId::class.java.desiredAssertionStatus()
//        "on", "" -> true
//        "off" -> false
//        else -> error("System property '$DEBUG_PROPERTY_NAME' has unrecognized value '$value'")
//    }
//}
//
///**
// * Executes a block using a given coroutine context.
// */
//internal inline fun <T> withCoroutineContext(context: CoroutineContext, block: () -> T): T {
//    val oldName = context.updateThreadContext()
//    try {
//        return block()
//    } finally {
//        restoreThreadContext(oldName)
//    }
//}
//
//@PublishedApi
//internal fun CoroutineContext.updateThreadContext(): String? {
//    if (!DEBUG) return null
//    val coroutineId = this[CoroutineId] ?: return null
//    val coroutineName = this[CoroutineName]?.name ?: "coroutine"
//    val currentThread = Thread.currentThread()
//    val oldName = currentThread.name
//    currentThread.name = buildString(oldName.length + coroutineName.length + 10) {
//        append(oldName)
//        append(" @")
//        append(coroutineName)
//        append('#')
//        append(coroutineId.id)
//    }
//    return oldName
//}
//
//@PublishedApi
//internal fun restoreThreadContext(oldName: String?) {
//    if (oldName != null) Thread.currentThread().name = oldName
//}
//
//private class CoroutineId(val id: Long) : AbstractCoroutineContextElement(CoroutineId) {
//    companion object Key : CoroutineContext.Key<CoroutineId>
//    override fun toString(): String = "CoroutineId($id)"
//}
