package com.ubertob.kondor.json

import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@JvmInline
value class TMillis(val value: Long) {
    companion object {
        fun fromEpochMillis(millis: Long): TMillis = TMillis(millis)
        fun now(): TMillis = TMillis(System.currentTimeMillis())
    }
}

class JsonConverterDinaturalTest {

    @Test
    fun `dinatural transforms Product to String while preserving JSON structure`() {
        // Create a dinatural transformation from Product to String
        val productToString = JProduct.dinatural(
            { str: String -> Product(str.hashCode(), "Product $str", "Created from string: $str", 99.99) },
            { product: Product -> "${product.id}-${product.shortDesc}" }
        )

        // Test serialization
        val inputString = "test-product"
        val json = productToString.toJson(inputString)
        val expectedJson =
            """{"id": ${inputString.hashCode()}, "long_description": "Created from string: $inputString", "short-desc": "Product $inputString", "price": 99.99}"""
        expectThat(json).isEqualTo(expectedJson)

        // Test deserialization
        val result = productToString.fromJson(json).expectSuccess()
        val expectedResult = "${inputString.hashCode()}-Product $inputString"
        expectThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `dinatural preserves null handling`() {
        val productToString = JProduct.dinatural(
            { str: String -> Product(str.hashCode(), str, "test product", if (str == "null-price") null else 99.99) },
            { product: Product -> if (product.price == null) "null-price" else product.shortDesc }
        )

        // Test with non-null price
        val json = """{"id": 123, "long_description": "test product", "short-desc": "test", "price": 99.99}"""
        val result = productToString.fromJson(json).expectSuccess()
        expectThat(result).isEqualTo("test")

        // Test with null price (the only nullable field)
        val nullableJson = """{"id": 456, "long_description": "test product", "short-desc": "test", "price": null}"""
        val nullableResult = productToString.fromJson(nullableJson).expectSuccess()
        expectThat(nullableResult).isEqualTo("null-price")
    }

    @Test
    fun `dinatural can create TMillis converter from JLong`() {
        val JTMillis = JLong.dinatural(
            { millis: TMillis -> millis.value },
            { value: Long -> TMillis(value) }
        )

        // Test serialization
        val timestamp = TMillis.fromEpochMillis(1234567890L)
        val json = JTMillis.toJson(timestamp)
        expectThat(json).isEqualTo("1234567890")

        // Test deserialization
        val parsed = JTMillis.fromJson(json).expectSuccess()
        expectThat(parsed).isEqualTo(timestamp)

        // Test with current time
        val now = TMillis.now()
        val nowJson = JTMillis.toJson(now)
        val parsedNow = JTMillis.fromJson(nowJson).expectSuccess()
        expectThat(parsedNow).isEqualTo(now)
    }

    @Test
    fun `dinatural composes with other transformations`() {
        val productToInt = JProduct.dinatural(
            { num: Int -> Product(num, "Product $num", "Number product", 99.99) },
            { product: Product -> product.id }
        )

        val intToString = productToInt.dinatural(
            { str: String -> str.length },
            { num: Int -> "Length: $num" }
        )

        val inputString = "test"
        val json = intToString.toJson(inputString)
        val expectedJson =
            """{"id": 4, "long_description": "Number product", "short-desc": "Product 4", "price": 99.99}"""
        expectThat(json).isEqualTo(expectedJson)

        val result = intToString.fromJson(json).expectSuccess()
        expectThat(result).isEqualTo("Length: 4")
    }
}
