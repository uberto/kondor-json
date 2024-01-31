package com.ubertob.kondor.json.jmh

import com.ubertob.kondor.json.JsonStyle
import com.ubertob.kondor.json.toJson
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

//add Moshi, Gson, KotlinX comparison

open class BenchmarkKondor {

    @Benchmark
    fun kondorSerializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) {
        with(testFix) {
            val json = jDemoClasses.toJson(objList)
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun kondorDeserializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) =
        with(testFix) {
            val list = jDemoClasses.fromJson(jsonString)
            blackHole.consume(list)
        }

}

fun main() {
    benchLoop(::toJson, ::fromJson)
}

private fun fromJson(it: String) = jDemoClasses.fromJson(it).orThrow()

private fun toJson(it: List<DemoClass>) =
    jDemoClasses.toJson(it, JsonStyle.compact)

//serialization:
// replace StringBuilder with better Writer with support to OutputStream

//deser:
// direct deser from Converter