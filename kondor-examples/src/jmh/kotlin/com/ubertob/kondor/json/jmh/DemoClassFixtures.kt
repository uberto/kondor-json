package com.ubertob.kondor.json.jmh

import com.ubertob.kondor.json.toNdJson
import com.ubertob.kondortools.chronoAndLog
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class DemoClassFixtures {
    val objList = (1..10).map {
        DemoClass.random()
    }.toList()

    val jsonString = jDemoClasses.toJson(objList)
    val ndJsonStrings = toNdJson(JDemoClass)(objList)
}



fun benchLoop(ser: (List<DemoClass>) -> String, deser: (String) -> List<DemoClass>) {
    val testFix = DemoClassFixtures()

    repeat(1_000) {
        chronoAndLog("iter $it") {
            repeat(10_000) {
//                val json = ser(testFix.objList)
                val list = deser(testFix.jsonString)
            }
        }
    }
}

fun benchLoopNd(ser: (List<DemoClass>) -> Sequence<String>, deser: (Sequence<String>) -> List<DemoClass>) {
    val testFix = DemoClassFixtures()

    repeat(1_000) {
        chronoAndLog("iter $it") {
            repeat(10_000) {
                val json = ser(testFix.objList)
                val list = deser(testFix.ndJsonStrings)
            }
        }
    }
}