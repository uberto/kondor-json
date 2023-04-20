package com.ubertob.kondortools


import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError
import com.ubertob.kondor.outcome.onFailure
import com.ubertob.kondor.outcome.recover
import org.junit.jupiter.api.fail


fun <T> Outcome<*, T>.expectSuccess(): T =
    this.onFailure { fail(it.msg) }

fun <E : OutcomeError> Outcome<E, *>.expectFailure(): E =
    this.transform { fail("Should have failed but was $it") }
        .recover { it }

fun <T> T.printIt(prefix: String = ""): T =
    also { println("$prefix$it") }


fun <T> chronoAndLog(logPrefix: String, fn: () -> T): T {
    val start = System.nanoTime()
    val res = fn()
    val elapsed = System.nanoTime() - start

    println("$logPrefix ${elapsed / 1_000_000} ms")
    return res
}
