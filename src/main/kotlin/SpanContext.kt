import io.opentracing.util.GlobalTracer
import io.opentracing.Span
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend fun <T> trace(parent: Span, block: suspend () -> T): T {

    val span = GlobalTracer.get().buildSpan("waitFor").asChildOf(parent).start()

    return withContext(SpanContextElement(span)) {
        try {
            block()
        } finally {
            span.finish()
        }
    }
}

class SpanContextElement(val span: Span? = GlobalTracer.get().activeSpan()) : ThreadContextElement<Span?> {
    override val key = Key

    // this is invoked before coroutine is resumed on current thread
    override fun updateThreadContext(context: CoroutineContext): Span? {
        val tracer = GlobalTracer.get()
        val previousSpan = tracer.activeSpan()

        return if (previousSpan !== span) {
            tracer.activateSpan(span)
            previousSpan
        } else {
            null
        }
    }

    // this is invoked after coroutine has suspended on current thread
    override fun restoreThreadContext(context: CoroutineContext, oldState: Span?) {
        oldState?.let {
            GlobalTracer.get().activateSpan(it)
        }
    }

    companion object Key : CoroutineContext.Key<SpanContextElement>
}
