package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.outcome.Failure
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class JRepresentableTest {
    object JStringGoesBoom : JStringRepresentable<String>() {
        override val cons: (String) -> String = { throw IllegalArgumentException("boom")}
        override val render: (String) -> String = { it }
    }

    object JIntGoesBoom : JIntRepresentable<String>() {
        override val cons: (Int) -> String = { throw IllegalArgumentException("boom")}
        override val render: (String) -> Int = { 1 }
    }

    object JBoolGoesBoom : JBooleanRepresentable<String>() {
        override val cons: (Boolean) -> String = { throw IllegalArgumentException("boom")}
        override val render: (String) -> Boolean = { true }
    }

    @Test
    fun `errors thrown in the constructor function for JStringRepresentable are captured as Outcomes`() {
        val json = "\"a-string\""

        val outcome = JStringGoesBoom.fromJson(json)

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.reason }
            .isEqualTo("boom")

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.path }
            .isEqualTo(NodePathRoot)
    }

    @Test
    fun `errors thrown in the constructor function for JNumberRepresentable are captured as Outcomes`() {
        val json = "1"

        val outcome = JIntGoesBoom.fromJson(json)

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.reason }
            .isEqualTo("boom")

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.path }
            .isEqualTo(NodePathRoot)
    }

    @Test
    fun `errors thrown in the constructor function for JBooleanRepresentable are captured as Outcomes`() {
        val json = "true"

        val outcome = JBoolGoesBoom.fromJson(json)

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.reason }
            .isEqualTo("boom")

        expectThat(outcome)
            .isA<Failure<JsonError>>()
            .get { error.path }
            .isEqualTo(NodePathRoot)
    }
}
