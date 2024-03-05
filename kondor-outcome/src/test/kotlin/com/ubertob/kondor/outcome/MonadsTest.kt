package com.ubertob.kondor.outcome

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MonadsTest {

    @Test
    fun bindingComposition() {

        val res: UnitOutcome = getUser(123)
            .bind { u ->
                getMailText(u.name)
                    .bind { t ->
                        sendEmailUser(u.email, t)
                    }
            }
        res.expectSuccess()
    }

    @Test
    fun bindingCompositionOnFailure() {

        val res = fun(): UnitOutcome {
            val u = getUser(123).onFailure { return it.asFailure() }
            val t = getMailText(u.name).onFailure { return it.asFailure() }
            val e = sendEmailUser(u.email, t).onFailure { return it.asFailure() }
            return e.asSuccess()
        }
        res().expectSuccess()

    }

    @Test
    fun `bindFailure allow to retry another way`() {
        val user = getUser(-23)
            .bindFailure { getUser(23) }
            .expectSuccess()

        expectThat(user.name).isEqualTo("user23")
    }

    @Test
    fun `bindAlso ignores the result of second fun if success`() {
        val user = getUser(42)
            .bindAlso { user -> sendEmailUser(user.email, "Hello ${user.name}") }
            .expectSuccess()

        expectThat(user.name).isEqualTo("user42")
    }

    @Test
    fun `bindAlso fails the result if second fun fails`() {
        val error = getUser(42)
            .bindAlso { user -> sendEmailUser(user.email, "") }
            .expectFailure()

        expectThat(error.msg).isEqualTo("empty text or email")
    }

}