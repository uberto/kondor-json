package com.ubertob.kondortools

import com.ubertob.kondor.json.JsonRenderer
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.outcome.OutcomeError
import com.ubertob.kondor.outcome.UnitOutcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind

data class MatcherError(val expected: String, val actual: String) : OutcomeError {
    override val msg: String = "Expected:\n $expected\n Actual: \n $actual"
}

infix fun String.isEquivalentJson(expected: String): UnitOutcome {
    return parseJsonNode(expected)
        .bind { j1 ->
            parseJsonNode(this).bind { j2 ->
                if (JsonRenderer.prettyIncludeNulls.render(j1) == JsonRenderer.prettyIncludeNulls.render(j2))
                    Unit.asSuccess()
                else
                    MatcherError(expected, this).asFailure()
            }
        }
}
