package com.ubertob.kondor.json.jmh

import kotlin.random.Random

data class DemoClass(
    val text: String,
    val boolean: Boolean,
    val double: Double,
    val nullableInt: Int?,
    val array: List<String>
) {
    companion object {
        fun random() = DemoClass(
            text = randomString(),
            boolean = Random.nextBoolean(),
            double = Random.nextDouble(),
            nullableInt =  Random.nextInt(), //TODO!!!! if (Random.nextBoolean()) else null,
            array = (1..Random.nextInt(5, 20)).map { randomString() }.toList()
        )
    }
}

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
fun randomString(): String =
    (1..Random.nextInt(3, 10))
        .map { charPool.random() }
        .joinToString(separator = "")