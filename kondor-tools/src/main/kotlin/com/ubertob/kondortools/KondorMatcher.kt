package com.ubertob.kondortools

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.json.parser.JsonLexer
import com.ubertob.kondor.json.parser.pretty
import com.ubertob.kondor.outcome.*


data class MatcherError(val expected: String, val actual: String) : OutcomeError {
    override val msg: String = "Expected:\n $expected\n Actual: \n $actual"
}


infix fun String.isSameJsonObject(expected: String): Outcome<OutcomeError, Unit> {
    return ObjectNode.parse(JsonLexer(expected).tokenize(), NodePathRoot)
        .bind { j1 ->
            ObjectNode.parse(JsonLexer(this).tokenize(), NodePathRoot).bind { j2 ->
                if (j1.pretty(true, 2) == j2.pretty(true, 2))
                    Unit.asSuccess()
                else
                    MatcherError(expected, this).asFailure()
            }
        }
}
