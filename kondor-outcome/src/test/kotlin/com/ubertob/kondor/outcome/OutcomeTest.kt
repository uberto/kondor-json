package com.ubertob.kondor.outcome

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import kotlin.random.Random

internal class OutcomeTest {

    //================ Basics

    @Test
    fun `transform change the result`() {

        val name = getUser(12)
            .transform { it.name.uppercase() }
            .expectSuccess()

        expectThat(name).isEqualTo("USER12")
    }

    @Test
    fun `transform only operate on success`() {

        val msg = getUser(-1)
            .transform { error("never arrives here!") }
            .expectFailure()
            .msg

        expectThat(msg).isEqualTo("wrong id")
    }

    @Test
    fun `transform failure only operate on failures`() {

        val name = getUser(21)
            .transformFailure { error("never arrives here!") }
            .expectSuccess()
            .name

        expectThat(name).isEqualTo("user21")
    }

    //================ Errors

    @Test
    fun `tryOrFail catches exceptions and converts them in failures`() {

        val msg = Outcome.tryOrFail {
            error("something is broken")
        }.expectFailure()
            .msg

        expectThat(msg).isEqualTo("something is broken")
    }

    @Test
    fun `orThrow launch an exception in case of failure`() {

        Assertions.assertThrows(OutcomeException::class.java) {
            getUser(-2)
                .orThrow()
                .name
        }
    }

    @Test
    fun `orNull returns null in case of failure`() {

        val name: String? = getUser(-12)
            .orNull()
            ?.name

        expectThat(name).isNull()
    }

    @Test
    fun `recover converts the failure to the same type of success`() {

        val name = getUser(-12)
            .recover { User("anom user", "") }
            .name

        expectThat(name).isEqualTo("anom user")

    }

    @Test
    fun `recover doesn't change the success`() {

        val name = getUser(12)
            .recover { error("not called!") }
            .name

        expectThat(name).isEqualTo("user12")
    }

    //================ Monads

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



    //convenience methods
    @Test
    fun `asOutcome transforms values into Outcomes`() {

        val okStr = "All ok"
        okStr.asOutcome({ contains("ok") }, ::Err).expectSuccess()

        val errStr = "There is an error!"
        errStr.asOutcome({ contains("error").not() }, ::Err).expectFailure()

    }

    @Test
    fun `failIf fails the outcome if a predicate is true`() {
        val error = getUser(100)
            .failIf({ it.name.length > 6 }, { Err("${it.name} is too long!") })
            .expectFailure()

        expectThat(error.msg).isEqualTo("user100 is too long!")
    }

    @Test
    fun `failUnless fails the outcome if a predicate is false`() {
        val error = getUser(100)
            .failUnless({ name.length <= 6 }, { Err("${it.name} is too long!") })
            .expectFailure()

        expectThat(error.msg).isEqualTo("user100 is too long!")
    }


    @Test
    fun `combine successes`() {
        val a = 2.asSuccess()
        val b = 3.asSuccess()

        val combined = a.combine(b)

        expectThat(combined).isEqualTo(Success(Pair(2, 3)))
    }

    val intFailure: Outcome<Err, Int> = Err("NAN").asFailure()
    @Test
    fun `combine success with failure`() {
        val two = 2.asSuccess()

        val combined = two.combine(intFailure).expectFailure()

        expectThat(combined).isEqualTo(Err(msg="NAN"))
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
    fun `failIfNull fails null successes`() {
        val value = 42.asSuccess()

        val res = value.transform { null }.failIfNull { Err("Null failure!") }.expectFailure()

        expectThat(res.msg).isEqualTo("Null failure!")
    }

    @Test
    fun `transformIfNotNull transform only non null values`() {

        val value: Success<Int?> = Success(42)

        val res = value.transform { null }
            .transformIfNotNull(Int::toString)
            .expectSuccess()
        expectThat(res).isEqualTo(null)

        val res2 = value
            .transformIfNotNull(Int::toString)
            .expectSuccess()
        expectThat(res2).isEqualTo("42")
    }

    @Test
    fun `withSuccess to handle sideeffects`() {
        var sideEffect: Int? = null

        val successOutcome = 42.asSuccess()

        val result = successOutcome.withSuccess { sideEffect = it }

        expectThat(result).isEqualTo(successOutcome)
        expectThat(sideEffect).isEqualTo(42)
    }

    @Test
    fun `withSuccess won't call sideEffects in case of failures`() {
        var sideEffect: Int? = null

        val result = intFailure.withSuccess { sideEffect = it }

        expectThat(result).isEqualTo(intFailure)
        expectThat(sideEffect).isEqualTo(null)
    }

    @Test
    fun `withFailure call sideEffect in case of failures`() {
        var sideEffect: String? = null

        val failureOutcome = Err("test error").asFailure()

        val result = failureOutcome.withFailure { sideEffect = it.msg }

        expectThat(result).isEqualTo(Failure(Err("test error")))
        expectThat(sideEffect).isEqualTo("test error")
    }



}