package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compactSorted
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.randomText
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.random.Random


private data class Example(
    val i: Int,
    val s: String
)

private object V1Format : JAny<Example>() {
    val i by num(Example::i)
    val s by str(Example::s)

    override fun JsonNodeObject.deserializeOrThrow(): Example =
        Example(i = +i, s = +s)
}

private object V2Format : JAny<Example>() {
    val int by num(Example::i)
    val str by str(Example::s)

    override fun JsonNodeObject.deserializeOrThrow(): Example =
        Example(i = +int, s = +str)
}

private val converterWithMandatoryVersionField = VersionMapConverter(
    versionConverters = mapOf("1" to V1Format, "2" to V2Format)
)

private val converterWithDefaultVersion = VersionMapConverter(
    versionConverters = mapOf("1" to V1Format, "2" to V2Format),
    defaultVersion = "1"
)

private val converterWithUnversionedVersions = VersionMapConverter(
    unversionedConverters = listOf(V2Format, V1Format),
)

private val convertorWithMixedVersions = VersionMapConverter(
    versionConverters = mapOf("2" to V2Format),
    unversionedConverters = listOf(V1Format),
)


class VersionedConverterTest {

    @Test
    fun `reads multiple versions, writes one version`() {
        val converter = converterWithMandatoryVersionField

        val original = Example(2, "eyes")

        val v1Json = """{"@version":"1","i":2,"s":"eyes"}"""
        val v2Json = """{"@version":"2","int":2,"str":"eyes"}"""

        expectThat(original).isEqualTo(converter.fromJson(v1Json).expectSuccess())
        expectThat(original).isEqualTo(converter.fromJson(v2Json).expectSuccess())

        val generatedJson = converter.toJson(original, compactSorted)
        expectThat(v2Json).isEqualTo(generatedJson)
    }

    @Test
    fun `fails when mandatory version is missing`() {
        val converter = converterWithMandatoryVersionField

        val jsonWithoutSpecificVersion = """{"i":3,"s":"amigos"}"""

        val failure = converter.fromJson(jsonWithoutSpecificVersion).expectFailure() as ConverterJsonError
        expectThat(failure.path).isEqualTo(NodePathRoot)
        expectThat("no valid versioned convertor").isEqualTo(failure.reason)
    }

    @Test
    fun `can use default version if not specified in the json`() {
        val converter = converterWithDefaultVersion

        val badJson = """{"i":3,"s":"amigos"}"""

        val parsed = converter.fromJson(badJson).expectSuccess()
        val expected = Example(3, "amigos")
        expectThat(expected).isEqualTo(parsed)
    }

    @Test
    fun `can use a fallback 'stack' of converter versions when the there is no explicit versioning of the data`() {
        val converter = converterWithUnversionedVersions

        val original = Example(2, "eyes")

        val badV1Json = """{"i":2,"s":"eyes"}"""
        val badV2Json = """{"int":2,"str":"eyes"}"""

        expectThat(original).isEqualTo(converter.fromJson(badV1Json).expectSuccess())
        expectThat(original).isEqualTo(converter.fromJson(badV2Json).expectSuccess())

        val generatedJson = converter.toJson(original, compactSorted)
        expectThat(badV2Json).isEqualTo(generatedJson)
    }

    @Test
    fun `will prefer the versions to the stack, and will settle on the versioned version`() {
        val converter = convertorWithMixedVersions

        val original = Example(2, "eyes")

        val badV1Json = """{"i":2,"s":"eyes"}"""
        val badV2Json = """{"@version":"2","int":2,"str":"eyes"}"""

        expectThat(original).isEqualTo(converter.fromJson(badV1Json).expectSuccess())
        expectThat(original).isEqualTo(converter.fromJson(badV2Json).expectSuccess())

        val generatedJson = converter.toJson(original, compactSorted)
        expectThat(badV2Json).isEqualTo(generatedJson)
    }

    @Test
    fun `generates json node equivalent to json text`() {
        val converter = converterWithMandatoryVersionField

        repeat(10) {
            val original = Example(Random.nextInt(), randomText(10))

            val directJson = converter.toJson(original, compactSorted)

            val parsed = converter.fromJson(directJson).expectSuccess()
            expectThat(parsed).isEqualTo(original)

            val jsonFromJsonNode =
                JJsonNode.toJson(converter.toJsonNode(original), compactSorted)

            expectThat(directJson).isEqualTo(jsonFromJsonNode)
        }
    }
}