package com.ubertob.kondor.json.jmh

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

open class BenchmarkKondorNdJson {

    @Benchmark
    fun kondorSerializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) {
        with(testFix) {
            val json = demoClassNdProducer(objList).toList()
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun kondorDeserializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) =
        with(testFix) {
            val list = demoClassNdConsumer(ndJsonStrings)
            blackHole.consume(list)
        }

}

fun main() {
    benchLoopNd(::toNdJson, ::fromNdJson)
}

private fun fromNdJson(lines: Sequence<String>): List<DemoClass> = demoClassNdConsumer(lines).orThrow()

private fun toNdJson(objs: List<DemoClass>): Sequence<String> =
    demoClassNdProducer(objs)
