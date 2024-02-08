package com.ubertob.kondor.outcome

import org.junit.jupiter.api.fail

fun <T> Outcome<*, T>.expectSuccess(): T =
    this.onFailure { fail(it.msg) }

fun <T, E : OutcomeError> Outcome<E, T>.expectFailure(): E =
    this.transform { fail("Should have failed but was $it") }
        .recover { it }