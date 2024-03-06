package com.ubertob.kondor.outcome

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import kotlin.random.Random

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

    @Test
    fun `Kleisli composition`() {
        val addTwo: (Int) -> Outcome<Err, Int> = { x -> (x + 2).asSuccess() }
        val multiplyThree: (Int) -> Outcome<Err, Int> = { x -> (x * 3).asSuccess() }

        val addTwoAndMultiplyThree = addTwo compose multiplyThree

        val result = addTwoAndMultiplyThree(2)

        expectThat(result).isEqualTo(Success(12))
    }

    @Test
    fun `join nested outcomes`() {
        val value = Random.nextInt().asSuccess()
        val nested = Success(value)

        val joined = nested.join()

        expectThat(joined).isEqualTo(value)
    }


    @Test
    fun `traverse a list of outcome to outcome of a list`() {

        val userIds = (1..20).toList()

        val outcome1 = userIds.traverse { getUser(it) }

        val outcome2 = userIds.map { getUser(it) }.extractList()

        expectThat(outcome1).isEqualTo(outcome2)

        expectThat( outcome1.expectSuccess()).hasSize(20)


    }

    @Test
    fun `traverse a sequence of outcome to outcome of a set`() {

        val userIds = (1..20).toList().asSequence()

        val outcome1 = userIds.traverseToSet { getUser(it) }

        val outcome2 = userIds.map { getUser(it) }.extractSet()

        expectThat(outcome1).isEqualTo(outcome2)

        expectThat( outcome1.expectSuccess()).hasSize(20)


    }

}
