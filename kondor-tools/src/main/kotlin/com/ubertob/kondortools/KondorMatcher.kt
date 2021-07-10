package com.ubertob.kondortools

import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.json.parser.pretty
import com.ubertob.kondor.outcome.*


data class MatcherError(val expected: String, val actual: String) : OutcomeError {
    override val msg: String = "Expected:\n $expected\n Actual: \n $actual"
}


infix fun String.isEquivalentJson(expected: String): UnitOutcome {
    return parseJsonNode(expected)
        .bind { j1 ->
            parseJsonNode(this).bind { j2 ->
                if (j1.pretty(true, 2) == j2.pretty(true, 2))
                    Unit.asSuccess()
                else
                    MatcherError(expected, this).asFailure()
            }
        }
}
