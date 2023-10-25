package com.ubertob.kondor.json.jmh

import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class DemoClassFixtures {
    val objList = (1..100).map {
        DemoClass.random()
    }.toList()

    val jsonString = jDemoClasses.toJson(objList)
}