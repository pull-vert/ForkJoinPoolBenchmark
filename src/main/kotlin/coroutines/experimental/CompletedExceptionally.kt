package coroutines.experimental

/**
 * Class for an internal state of a job that had completed exceptionally, including cancellation.
 *
 * **Note: This class cannot be used outside of internal coroutines framework**.
 *
 * @param cause the exceptional completion cause. If `cause` is null, then an exception is
 *        if created via [createException] on first get from [exception] property.
 * @param allowNullCause if `null` cause is allowed.
 * @suppress **This is unstable API and it is subject to change.**
 */
public open class CompletedExceptionally protected constructor(
        public val cause: Throwable?,
        allowNullCause: Boolean
) {
    /**
     * Creates exceptionally completed state.
     * @param cause the exceptional completion cause.
     */
    public constructor(cause: Throwable) : this(cause, false)

    @Volatile
    private var _exception: Throwable? = cause // will materialize JobCancellationException on first need

    init {
        require(allowNullCause || cause != null) { "Null cause is not allowed" }
    }

    /**
     * Returns completion exception.
     */
    public val exception: Throwable get() =
        _exception ?: // atomic read volatile var or else create new
        createException().also { _exception = it }

    protected open fun createException(): Throwable = error("Completion exception was not specified")

    override fun toString(): String = "${this::class.java.simpleName}[$exception]"
}