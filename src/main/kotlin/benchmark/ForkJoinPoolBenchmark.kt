package benchmark

import org.openjdk.jmh.annotations.Benchmark

const val N = 1_000_000
fun Int.isGood() = this % 4 == 0

open class ForkJoinPoolBenchmark {
        @Benchmark
    fun testBaselineLoop(): Int {
        var sum = 0
        for (i in 1..N) {
            if (i.isGood())
                sum += i
        }
        return sum
    }
}