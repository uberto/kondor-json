package com.ubertob.kondortools

import com.ubertob.kondor.json.JsonStyle
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.outcome.*

data class MatcherError(val expected: String, val actual: String) : OutcomeError {
    override val msg: String = "Expected:\n $expected\n Actual: \n $actual"
}

infix fun String.isEquivalentJson(expected: String): UnitOutcome {
    return parseJsonNode(expected)
        .bind { j1 ->
            parseJsonNode(this).bind { j2 ->
                val render1 = JsonStyle.prettyWithNulls.render(j1)
                val render2 = JsonStyle.prettyWithNulls.render(j2)
                if (render1 == render2)
                    Unit.asSuccess()
                else
                    MatcherError(render1, render2).asFailure()
            }
        }
}
