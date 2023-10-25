package com.ubertob.kondor.json.jmh

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule


object JacksonDsl {

    private val objectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, true)
            .build()
    )

    val nodeFactory = JsonNodeFactory.instance

    fun toJson(demoList: List<DemoClass>): String {
        val arrayNode = nodeFactory.arrayNode()
        demoList.forEach { arrayNode.add(demoClassToJsonNode(it)) }
        return arrayNode.toString()

    }

    private fun demoClassToJsonNode(instance: DemoClass): ObjectNode {
        val node = nodeFactory.objectNode()
        node.put("text", instance.text)
        node.put("boolean", instance.boolean)
        node.put("double", instance.double)
        if (instance.nullableInt != null) {
            node.put("nullableInt", instance.nullableInt)
        }
        val arrayNode = node.putArray("array")
        instance.array.forEach { arrayNode.add(it) }
        return node
    }

    private fun jsonNodeToDemoClass(node: JsonNode): DemoClass {
        val text = node["text"].asText()
        val boolean = node["boolean"].asBoolean()
        val double = node["double"].asDouble()
        val nullableInt = node["nullableInt"]?.asInt()
        val array = mutableListOf<String>()
        node["array"].forEach { item -> array.add(item.asText()) }
        return DemoClass(text, boolean, double, nullableInt, array)
    }

    fun fromJson(json: String): List<DemoClass> {
        val rootNode = objectMapper.readTree(json) as ArrayNode
        return rootNode.map { jsonNodeToDemoClass(it) }
    }

}

fun main() {
    val demoClasses = listOf(DemoClass.random(), DemoClass.random(), DemoClass.random())
    val json = JacksonDsl.toJson(demoClasses)
    println(json)
    val parsed = JacksonDsl.fromJson(json)
    println(parsed == demoClasses)
}