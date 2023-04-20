package com.ubertob.kondor


import org.leadpony.justify.api.JsonSchema
import org.leadpony.justify.api.JsonValidationService
import kotlin.random.Random

const val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val lowercase = "abcdefghijklmnopqrstuvwxyz"
const val digits = "0123456789"
const val spacesigns = " ,.:+-()%$@{}[]\\\"\n\r\t"
const val latin1 = "°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"
const val japanese = "こんにちは　世界"

const val text = uppercase + lowercase + digits + spacesigns

fun stringsGenerator(charSet: String, minLen: Int, maxLen: Int): Sequence<String> = generateSequence {
    randomString(charSet, minLen, maxLen)
}

fun randomString(charSet: String, minLen: Int, maxLen: Int) =
    StringBuilder().run {
        val len = randomInBetween(maxLen, minLen)
        repeat(len) {
            append(charSet.random())
        }
        toString()
    }

private fun randomInBetween(maxLen: Int, minLen: Int) =
    if (maxLen > minLen) Random.nextInt(maxLen - minLen) + minLen else minLen

fun randomText(len: Int) = randomString(text, len, len)

fun <T> randomList(minLen: Int, maxLen: Int, f: (index: Int) -> T): List<T> =
    (0..randomInBetween(maxLen, minLen)).map { f(it) }

fun <T> randomNullable(f: () -> T): T? = if (Random.nextBoolean()) f() else null

fun randomPrice(min: Int, max: Int) = Random.nextInt(min * 100, max * 100) / 100.0



private val service = JsonValidationService.newInstance()

private fun schemaService(schemaJson: String): JsonSchema =
    service.readSchema(schemaJson.byteInputStream())


fun validateJsonAgainstSchema(schemaJson: String, json: String) {
    val jsonConfig = schemaService(schemaJson)


    val handler = service.createProblemPrinter { error("Schema validation error: $it") }
    service.createReader(json.byteInputStream(), jsonConfig, handler)
        .use { reader -> reader.readValue() }
}

fun <T> chronoAndLog(logPrefix: String, fn: () -> T): T {
    val start = System.nanoTime()
    val res = fn()
    val elapsed = System.nanoTime() - start

    println("$logPrefix ${elapsed / 1_000_000} ms")
    return res
}