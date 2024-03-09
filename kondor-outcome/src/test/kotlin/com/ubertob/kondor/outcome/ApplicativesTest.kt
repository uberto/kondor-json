package com.ubertob.kondor.outcome

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class ApplicativesTest {

    @Test
    fun `transform2 maps a function with two arguments`() {
        val a = 2.asSuccess()
        val b = 3.asSuccess()

        val result = Outcome.transform2(a, b) { x, y -> x * y }

        expectThat(result).isEqualTo(Success(6))
    }

    @Test
    fun `! allow to partially apply an outcome to a function`() {
        val add: (Int, Int) -> Int = { x, y -> x + y }
        val a = 2.asSuccess()

        val partiallyApplied = add `!` a

        expectThat(partiallyApplied.expectSuccess()(5)).isEqualTo(7)
    }

    @Test
    fun `star add an outcome to a partial function`() {
        val addPartial = { y: Int -> 2 + y }.asSuccess()
        val b = 3.asSuccess()

        val result = addPartial `*` b

        expectThat(result).isEqualTo(Success(5))
    }

    @Test
    fun `star and bang also work with lambda outcomes (lazily)`() {
        val add3: (Int, Int, Int) -> Int = { x, y, z -> x + y + z }
        val a = { 3.asSuccess() }
        val b = { 4.asSuccess() }
        val c = { 5.asSuccess() }

        val result = add3 `!` a `*` b `*` c

        expectThat(result).isEqualTo(12.asSuccess())
    }

    @Test
    fun `lazy star and bang won't evaluate if failed before`() {
        val add3: (Int, Int, Int) -> Int = { x, y, z -> x + y + z }
        val a = { 3.asSuccess() }
        val b = { intFailure }
        val c = { error("This should not be called!") }

        val result = add3 `!` a `*` b `*` c

        expectThat(result).isEqualTo(intFailure)
    }
    @Test
    fun `castOrFail allow for subcasting`() {
        val person: BaseOutcome<Person> = getUser(12)

        val user = person.castOrFail { value -> Err("Cannot cast $value to User") }.orThrow()

        expectThat(user).isA<User>()
    }


    @Test
    fun `test map on Outcome collection`() {
        val values = listOf(1, 2, 3, 4, 5)
        val outcomeValues = values.asSuccess()

        val squaredOutcomeValues = outcomeValues.map { it * it }

        expectThat(squaredOutcomeValues).isEqualTo(Success(listOf(1, 4, 9, 16, 25)))
    }

    @Test
    fun `test flatMap on Outcome collection`() {
        val values = listOf(1, 2, 3, 4, 5)
        val outcomeValues = values.asSuccess()

        val outcomeFlatMap = outcomeValues.flatMap { (it * 2).asSuccess() }

        expectThat(outcomeFlatMap).isEqualTo(Success(listOf(2, 4, 6, 8, 10)))
    }
}