package com.ubertob.kondor.json.jmh

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue


object JacksonReflection {

    private val objectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, true)
            .build()
    )

    fun toJson(list: List<DemoClass>): String {
        return objectMapper.writeValueAsString(list)
    }

    fun fromJson(json: String): List<DemoClass> {
        return objectMapper.readValue(json)
    }

}

fun main() {
    val demoClasses = listOf(DemoClass.random(), DemoClass.random(), DemoClass.random())
    val json = JacksonReflection.toJson(demoClasses)
    println(json)
    val parsed = JacksonReflection.fromJson(json)
    println(parsed == demoClasses)
}