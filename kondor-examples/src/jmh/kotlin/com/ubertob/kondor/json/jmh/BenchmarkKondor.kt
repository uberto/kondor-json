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

/*

30/10/2023 on my laptop

BenchmarkJackson.jacksonDslDeserializationOfDemoClass         thrpt   20  11023.454 ± 206.814  ops/s
BenchmarkJackson.jacksonDslSerializationOfDemoClass           thrpt   20  18026.272 ± 167.600  ops/s
BenchmarkJackson.jacksonReflectionDeserializationOfDemoClass  thrpt   20   6259.493 ±  87.607  ops/s
BenchmarkJackson.jacksonReflectionSerializationOfDemoClass    thrpt   20  24324.008 ± 523.996  ops/s
BenchmarkKondor.kondorDeserializationOfDemoClass              thrpt   20   2605.131 ±   6.513  ops/s
BenchmarkKondor.kondorSerializationOfDemoClass                thrpt   20   4546.243 ±  95.795  ops/s

 */