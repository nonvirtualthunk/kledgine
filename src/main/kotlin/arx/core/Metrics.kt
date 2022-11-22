package arx.core

import com.codahale.metrics.*
import java.util.concurrent.TimeUnit


//class Timer {
//
//    fun record(measurementNanos: Long) {
//
//    }
//
//    fun <T>time(stmt: () -> T) : T {
//        val start = System.nanoTime()
//
//        try {
//            stmt()
//        } finally {
//            record(System.nanoTime() - start)
//        }
//    }
//}


object Metrics {
    val registry = MetricRegistry()

    fun timer(name: String) : Timer {
        return registry.timer(name)
    }

    fun counter(name: String) : Counter {
        return registry.counter(name)
    }

    fun meter(name: String) : Meter {
        return registry.meter(name)
    }

    fun print(nameFilter : String? = null) {
        val reporter: ConsoleReporter = ConsoleReporter.forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .disabledMetricAttributes(setOf(MetricAttribute.M15_RATE, MetricAttribute.M1_RATE, MetricAttribute.M5_RATE, MetricAttribute.MEAN_RATE, MetricAttribute.P95, MetricAttribute.P98))
            .filter(if (nameFilter == null) { MetricFilter.ALL } else { MetricFilter.contains(nameFilter) })
            .build()
        reporter.report()
    }
}


inline fun <T>Timer.timeStmt(stmt: () -> T) : T {
    val ctx = this.time()
    try {
        return stmt()
    } finally {
        ctx.stop()
    }
}