package com.ubertob.kondor.json.jmh

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

//Jackson, Moshi, Gson, KotlinX comparison


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

fun main(){
    benchLoop(jDemoClasses::toJson){ jDemoClasses.fromJson(it).orThrow()}
}

/*
Serialization Improvements:
improve numbers rendering skipping bigdecimal
improve string using a buffer and a custom writer
skip node intermediate step and generate directly string from object

 */