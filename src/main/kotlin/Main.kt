import datadog.trace.api.Trace
import io.opentracing.Span
import io.opentracing.util.GlobalTracer
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val spanRegex = Regex("""(\[ t_id=\d+, s_id=\d+, p_id=\d+]).+(duration_ns=\d+).*""")

object Main {
    private val logger: Logger = LoggerFactory.getLogger(Main.javaClass)
    @JvmStatic
    @Trace
    fun main(args: Array<String>) = runBlocking {
        val tracer = GlobalTracer.get()
        val span = tracer.activeSpan()

        logger.info("=== INIT === ${span.ddSpan()}")
        coroutineScope {
            val deferred: MutableList<Deferred<Int>> = mutableListOf()
            val jobs: MutableList<Job> = mutableListOf()

            repeat(10) {
                val newResult = ddAsync(it.toString()) {
                    val lastItem = if (it > 1) {
                        deferred[it - 1]
                    } else {
                        null
                    }
                    waitFor(1000, lastItem, it)
                }
                deferred.add(newResult)
            }
            repeat(10) {
                val job = ddLaunch("launch $it") {
                    val lastItem = if (it > 1) {
                        jobs[it - 1]
                    } else {
                        null
                    }
                    waitFor(1000, lastItem, it)
                }
                jobs.add(job)
            }
        }
    }

    private suspend fun waitFor(millis: Long, asyncResult: Deferred<Int>?, iteration: Int) : Int {
        var tracer = GlobalTracer.get()
        var span = tracer.activeSpan()
        span.setTag("Iteration", iteration)
        logger.info("++++ IN +++: $iteration; ${span.ddSpan()}")
        asyncResult?.await()
        tracer = GlobalTracer.get()
        span = tracer.activeSpan()
        logger.info("--- OUT ---: $iteration; ${span.ddSpan()}")
        delay(millis)
        return 1
    }

    private suspend fun waitFor(millis: Long, asyncResult: Job?, iteration: Int) : Int {
        var tracer = GlobalTracer.get()
        var span = tracer.activeSpan()
        span.setTag("Iteration", iteration)
        logger.info("++++ IN +++: $iteration; ${span.ddSpan()}")
        asyncResult?.join()
        tracer = GlobalTracer.get()
        span = tracer.activeSpan()
        logger.info("--- MIDDLE ---: $iteration; ${span.ddSpan()}")
        delay(millis)
        logger.info("--- OUT ---: $iteration; ${span.ddSpan()}")
        return 1
    }
}
