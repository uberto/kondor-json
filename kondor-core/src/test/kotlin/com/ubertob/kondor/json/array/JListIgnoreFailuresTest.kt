package com.ubertob.kondor.json.array

import com.ubertob.kondor.json.JInt
import com.ubertob.kondor.json.jsonnode.JsonNodeArray
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondortools.expectSuccess
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import org.junit.jupiter.api.Test

class JListIgnoreFailuresTest {
    val aMixedBagJsonNode = JsonNodeArray(
        listOf(
            JsonNodeNumber(1),
            JsonNodeNumber(2),
            JsonNodeString("two-and-a-half"),
            JsonNodeNumber(3)
        )
    )

    val anUnmixedBag = listOf(1,2,3)

    val converter = JListIgnoreFailures(JInt)

    @Test
    fun `ignores failures in the collection type conversion`() {
        val deserialized = converter.fromJsonNodeBase(aMixedBagJsonNode).expectSuccess()
        expectThat(deserialized).isEqualTo(anUnmixedBag)
    }

    @Test
    fun `still serializes normally`() {
        val json = converter.toJson(anUnmixedBag)
        val deserialized = converter.fromJson(json).expectSuccess()

        expectThat(deserialized).isEqualTo(anUnmixedBag)
    }
}
