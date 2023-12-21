package com.ubertob.kondor.json.jmh

import com.ubertob.kondortools.chronoAndLog
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

//Jackson, Moshi, Gson, KotlinX comparison


open class BenchmarkJackson {

    @Benchmark
    fun jacksonReflectionSerializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) {
        with(testFix) {
            val json = JacksonReflection.toJson(objList)
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun jacksonReflectionDeserializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) =
        with(testFix) {
            val list = JacksonReflection.fromJson(jsonString)
            blackHole.consume(list)
        }


    @Benchmark
    fun jacksonDslSerializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) {
        with(testFix) {
            val json = JacksonDsl.toJson(objList)
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun jacksonDslDeserializationOfDemoClass(blackHole: Blackhole, testFix: DemoClassFixtures) =
        with(testFix) {
            val list = JacksonDsl.fromJson(jsonString)
            blackHole.consume(list)
        }

}

fun main(){
    val testFix = DemoClassFixtures()

    repeat(1_000){
        chronoAndLog("iter $it") {
            repeat(1_000) {
                val json = JacksonDsl.toJson(testFix.objList)
                val list = JacksonDsl.fromJson(testFix.jsonString)
            }
        }
    }
}