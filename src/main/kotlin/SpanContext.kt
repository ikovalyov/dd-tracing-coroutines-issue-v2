import datadog.trace.core.DDSpan
import io.opentracing.util.GlobalTracer
import io.opentracing.Span
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

fun <T> CoroutineScope.ddAsync(traceName: String?, block: suspend () -> T) : Deferred<T> {
    val parentSpan = this.coroutineContext[SpanContextElement.Key]?.span
    val spanBuilder = GlobalTracer.get().buildSpan(traceName)
    parentSpan?.let {
        spanBuilder.asChildOf(it)
    }
    val span = spanBuilder.start()!!
    return this.async(SpanContextElement(span)) {
        try {
            block()
        } finally {
            span.finish()
            println(span.ddSpan())
        }
    }
}

fun <T>CoroutineScope.ddLaunch(traceName: String?, block: suspend () -> T) : Job {
    val parentSpan = this.coroutineContext[SpanContextElement.Key]?.span
    val spanBuilder = GlobalTracer.get().buildSpan(traceName)
    parentSpan?.let {
        spanBuilder.asChildOf(it)
    }
    val span = spanBuilder.start()!!
    return this.launch(SpanContextElement(span)) {
        try {
            block()
        } finally {
            span.finish()
            println(span.ddSpan())
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

fun Span.ddSpan(): Any {
    val delegateField = this::class.java.declaredFields.find {
        it.name == "delegate"
    }!!
    delegateField.trySetAccessible()
    return delegateField.get(this)
}