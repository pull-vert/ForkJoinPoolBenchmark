package coroutines.experimental

import kotlin.coroutines.experimental.Continuation

internal val Any.hexAddress: String
    get() = Integer.toHexString(System.identityHashCode(this))

// **KLUDGE**: there is no reason to include continuation into debug string until the following ticket is resolved:
// KT-18986 Debug-friendly toString implementation for CoroutineImpl
// (the current string representation of continuation is useless and uses buggy reflection internals)
// So, this function is a replacement that extract a usable information from continuation -> its class name, at least
internal fun Continuation<*>.toDebugString(): String = when (this) {
    is DispatchedContinuation -> toString()
    else -> "${this::class.java.name}@$hexAddress"
}