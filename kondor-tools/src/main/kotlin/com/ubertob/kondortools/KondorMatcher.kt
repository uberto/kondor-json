package com.ubertob.kondortools

import JsonLexer
import com.ubertob.kondor.json.NodePathRoot
import com.ubertob.kondor.json.ObjectNode
import com.ubertob.kondor.json.pretty
import com.ubertob.kondor.outcome.*


data class MatcherError(val expected: String, val actual: String) : OutcomeError {
    override val msg: String = "Expected:\n $expected\n Actual: \n $actual"
}


infix fun String.isSameJsonObject(expected: String): Outcome<OutcomeError, Unit> {
    return ObjectNode.parse(JsonLexer(expected).tokenize(), NodePathRoot)
        .bind { j1 ->
            ObjectNode.parse(JsonLexer(this).tokenize(), NodePathRoot).bind { j2 ->
                if (j1.pretty(2) == j2.pretty(2))
                    Unit.asSuccess()
                else
                    MatcherError(expected, this).asFailure()
            }
        }
}
