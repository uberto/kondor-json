package com.ubertob.kondor.json.jmh

import com.ubertob.kondortools.chronoAndLog
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

    val testFix = DemoClassFixtures()

    repeat(1_000){
        chronoAndLog("iter $it") {
            repeat(1_000) {
                val json = jDemoClasses.toJson(testFix.objList)
//                val list = jDemoClasses.fromJson(testFix.jsonString)
            }
        }
    }

}
