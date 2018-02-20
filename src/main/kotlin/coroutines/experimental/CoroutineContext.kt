package coroutines.experimental

import co.paralleluniverse.strands.Strand
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext

private const val DEBUG_PROPERTY_NAME = "kotlinx.coroutines.debug"

private val DEBUG = run {
    val value = try { System.getProperty(DEBUG_PROPERTY_NAME) }
    catch (e: SecurityException) { null }
    when (value) {
        "auto", null -> CoroutineId::class.java.desiredAssertionStatus()
        "on", "" -> true
        "off" -> false
        else -> error("System property '$DEBUG_PROPERTY_NAME' has unrecognized value '$value'")
    }
}

/**
 * Executes a block using a given coroutine context.
 */
internal inline fun <T> withCoroutineContext(context: CoroutineContext, block: () -> T): T {
    val oldName = context.updateThreadContext()
    try {
        return block()
    } finally {
        restoreThreadContext(oldName)
    }
}

//internal val CoroutineContext.coroutineName: String? get() {
//    if (!DEBUG) return null
//    val coroutineId = this[CoroutineId] ?: return null
//    val coroutineName = this[CoroutineName]?.name ?: "coroutine"
//    return "$coroutineName#${coroutineId.id}"
//}

@PublishedApi
internal fun CoroutineContext.updateThreadContext(): String? {
    if (!DEBUG) return null
    val coroutineId = this[CoroutineId] ?: return null
    val coroutineName = this[CoroutineName]?.name ?: "coroutine"
    val currentStrand = Strand.currentStrand()
    val oldName = currentStrand.name
    currentStrand.name = buildString(oldName.length + coroutineName.length + 10) {
        append(oldName)
        append(" @")
        append(coroutineName)
        append('#')
        append(coroutineId.id)
    }
    return oldName
}

@PublishedApi
internal fun restoreThreadContext(oldName: String?) {
    if (oldName != null) Strand.currentStrand().name = oldName
}

private class CoroutineId(val id: Long) : AbstractCoroutineContextElement(CoroutineId) {
    companion object Key : CoroutineContext.Key<CoroutineId>
    override fun toString(): String = "CoroutineId($id)"
}
