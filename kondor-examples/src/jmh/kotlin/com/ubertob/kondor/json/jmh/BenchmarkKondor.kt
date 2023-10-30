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


jacksonDslSerializationOfDemoClass
17151.220 ops/s

jacksonDslDeserializationOfDemoClass
11024.520 ops/s

jacksonReflectionDeserializationOfDemoClass
 6530.603 ops/s

jacksonReflectionSerializationOfDemoClass
25545.446 ops/s



kondorDeserializationOfDemoClass
2528.502 ops/s

kondorSerializationOfDemoClass
4226.918 ops/s

 */