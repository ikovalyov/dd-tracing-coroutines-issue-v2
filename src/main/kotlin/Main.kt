import datadog.trace.api.Trace
import io.opentracing.util.GlobalTracer
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Main {
    private val logger: Logger = LoggerFactory.getLogger(Main.javaClass)
    @JvmStatic
    @Trace
    fun main(args: Array<String>) = runBlocking {
        val tracer = GlobalTracer.get()
        val span = tracer.activeSpan()
        logger.info("${Thread.currentThread().name} $span")
        coroutineScope {
            val deferred: MutableList<Deferred<Int>> = mutableListOf()
            repeat(10) {
                val newResult = async(Dispatchers.IO) {
                    val lastItem = if (it > 1) {
                        deferred[it - 2]
                    } else {
                        null
                    }
                    waitFor(1000, lastItem, it)
                }
                deferred.add(newResult)
            }
        }
    }

    @Trace
    private suspend fun waitFor(millis: Long, asyncResult: Deferred<Int>?, iteration: Int) : Int {
        var tracer = GlobalTracer.get()
        var span = tracer.activeSpan()
        span.setTag("Iteration", iteration)
        logger.info("${Thread.currentThread().name} $span")
        asyncResult?.await()
        tracer = GlobalTracer.get()
        span = tracer.activeSpan()
        logger.info("${Thread.currentThread().name} $span")
        delay(millis)
        return 1
    }
}