package coroutines.experimental

import java.util.concurrent.CancellationException
import kotlin.coroutines.experimental.CoroutineContext

interface Job : CoroutineContext.Element {
    /**
     * Key for [Job] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<Job> {
        init {
            /*
             * Here we make sure that CoroutineExceptionHandler is always initialized in advance, so
             * that if a coroutine fails due to StackOverflowError we don't fail to report this error
             * trying to initialize CoroutineExceptionHandler
             */
            CoroutineExceptionHandler
        }
    }

    /**
     * Returns `true` when this job is active -- it was already started and has not completed or cancelled yet.
     * The job that is waiting for its [children] to complete is still considered to be active if it
     * was not cancelled.
     */
    public val isActive: Boolean

    /**
     * Cancels this job with an optional cancellation [cause]. The result is `true` if this job was
     * cancelled as a result of this invocation and `false` otherwise
     * (if it was already _completed_ or if it is [NonCancellable]).
     * Repeated invocations of this function have no effect and always produce `false`.
     *
     * When cancellation has a clear reason in the code, an instance of [CancellationException] should be created
     * at the corresponding original cancellation site and passed into this method to aid in debugging by providing
     * both the context of cancellation and text description of the reason.
     */
    public fun cancel(cause: Throwable? = null): Boolean

    /**
     * Returns [CancellationException] that signals the completion of this job. This function is
     * used by [cancellable][suspendCancellableCoroutine] suspending functions. They throw exception
     * returned by this function when they suspend in the context of this job and this job becomes _complete_.
     *
     * This function returns the original [cancel] cause of this job if that `cause` was an instance of
     * [CancellationException]. Otherwise (if this job was cancelled with a cause of a different type, or
     * was cancelled without a cause, or had completed normally), an instance of [JobCancellationException] is
     * returned. The [JobCancellationException.cause] of the resulting [JobCancellationException] references
     * the original cancellation cause that was passed to [cancel] function.
     *
     * This function throws [IllegalStateException] when invoked on a job that has not
     * [completed][isCompleted] nor [cancelled][isCancelled] yet.
     */
    public fun getCancellationException(): CancellationException
}