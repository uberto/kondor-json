package com.ubertob.kondor

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError
import com.ubertob.kondor.outcome.onFailure
import com.ubertob.kondor.outcome.recover
import org.junit.jupiter.api.fail

import kotlin.random.Random

fun <T : Any> Outcome<*, T>.expectSuccess(): T =
    this.onFailure { fail(it.msg) }

fun <E : OutcomeError> Outcome<E, *>.expectFailure(): E =
    this.transform { fail("Should have failed but was $it") }
        .recover { it }


const val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val lowercase = "abcdefghijklmnopqrstuvwxyz"
const val digits = "0123456789"
const val spacesigns = " ,.:+-()%$@{}[]\"\n\r"
const val text = lowercase + digits + spacesigns

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
