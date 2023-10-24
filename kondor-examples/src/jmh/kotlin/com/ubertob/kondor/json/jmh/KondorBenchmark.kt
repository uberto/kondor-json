package com.ubertob.kondor.json.jmh

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

//Jackson, Moshi, Gson, KotlinX comparison

object JDemoClass : JAny<DemoClass>() {
    val text_field by str(DemoClass::text)
    val `bool-value` by bool(DemoClass::boolean)
    val intNull by num(DemoClass::nullableInt)
    val doubleNum by num(DemoClass::double)
    val list_of_strings by array(DemoClass::array)

    override fun JsonNodeObject.deserializeOrThrow() =
        DemoClass(
            text = +text_field,
            boolean = +`bool-value`,
            nullableInt = +intNull,
            double = +doubleNum,
            array = +list_of_strings
        )
}

val jDemoClasses = JList(JDemoClass)

@State(Scope.Benchmark)
open class DemoClassFixtures {
    val objList = (1..100).map {
        DemoClass.random()
    }.toList()

    val jsonString = jDemoClasses.toJson(objList)
}

open class KondorBenchmark {

    @Benchmark
    fun `kondor serialization of demoClass`(blackHole: Blackhole, fif: DemoClassFixtures) {
        with(fif) {
            val json = jDemoClasses.toJson(objList)
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun `kondor deserialization of demoClass`(blackHole: Blackhole, fif: DemoClassFixtures) =
        with(fif) {
            val list = jDemoClasses.fromJson(jsonString)
            blackHole.consume(list)
        }

}
