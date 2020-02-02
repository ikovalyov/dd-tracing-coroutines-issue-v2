import datadog.trace.api.Trace
import io.opentracing.Span
import io.opentracing.util.GlobalTracer
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val spanRegex = Regex("""(\[ t_id=\d+, s_id=\d+, p_id=\d+]).+(ns=\d+).*""")

object Main {
    private val logger: Logger = LoggerFactory.getLogger(Main.javaClass)
    @JvmStatic
    @Trace
    fun main(args: Array<String>) = runBlocking {
        val tracer = GlobalTracer.get()
        val span = tracer.activeSpan()

        logger.info("=== INIT === ${span.print()}}")
        coroutineScope {
            val deferred: MutableList<Deferred<Int>> = mutableListOf()

            repeat(10) {
                val newResult = async(Dispatchers.IO) {
                    val lastItem = if (it > 1) {
                        deferred[it - 1]
                    } else {
                        null
                    }
                    trace(span) {
                        waitFor(1000, lastItem, it)
                    }
                }
                deferred.add(newResult)
            }
        }
    }

    private suspend fun waitFor(millis: Long, asyncResult: Deferred<Int>?, iteration: Int) : Int {
        var tracer = GlobalTracer.get()
        var span = tracer.activeSpan()

        span.setTag("Iteration", iteration)

        logger.info("++++ IN +++: $iteration; ${span.print()}")


        asyncResult?.await()
        tracer = GlobalTracer.get()
        span = tracer.activeSpan()
        logger.info("--- OUT ---: $iteration; ${span.print()}")
        delay(millis)
        return 1
    }
}

fun Span.print(): String = with(spanRegex.find(this.toString())) {
    return if (this != null) {
        val (a, _) = this.destructured
        return a
    } else {
        ""
    }
}
