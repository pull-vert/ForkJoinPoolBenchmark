package coroutines.experimental

internal class DispatchException actual constructor(message: String, cause: Throwable) : RuntimeException(message, cause)