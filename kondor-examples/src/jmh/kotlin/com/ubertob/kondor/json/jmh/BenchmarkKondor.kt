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

private fun fromJson(json: String) =
    jDemoClasses.fromJson(json).orThrow() //!!!
//    KondorTokenizer.tokenize(json.byteInputStream())
//        .bind(jDemoClasses::parseAndConvert).orThrow()
private fun toJson(objs: List<DemoClass>) =
    jDemoClasses.toJson(objs, JsonStyle.compact)

//serialization:
// add direct support for OutputStream instead of StringBuilder

//deser:
// direct deser: taking adventage of number type

