package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


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


class VersionedConverterTest {
    companion object {
        val testStyle = JsonStyle.compact.copy(sortedObjectFields = true)
    }
    
    @Test
    fun `reads multiple versions, writes one version`() {
        val converter = converterWithMandatoryVersionField
        
        val original = Example(2, "eyes")
        
        val v1Json = """{"@version":"1","i":2,"s":"eyes"}"""
        val v2Json = """{"@version":"2","int":2,"str":"eyes"}"""
        
        assertEquals(original, converter.fromJson(v1Json).expectSuccess())
        assertEquals(original, converter.fromJson(v2Json).expectSuccess())
        
        val generatedJson = converter.toJson(original, testStyle)
        assertEquals(v2Json, generatedJson)
    }
    
    @Test
    fun `fails when mandatory version is missing`() {
        val converter = converterWithMandatoryVersionField
        
        val jsonWithoutSpecificVersion = """{"i":3,"s":"amigos"}"""
        
        val failure = converter.fromJson(jsonWithoutSpecificVersion).expectFailure() as JsonPropertyError
        assertEquals(NodePathRoot, failure.path)
        assertEquals(converter.versionProperty, failure.propertyName)
    }
    
    @Test
    fun `can use default version if not specified in the json`() {
        val converter = converterWithDefaultVersion
        
        val badJson = """{"i":3,"s":"amigos"}"""
        
        val parsed = converter.fromJson(badJson).expectSuccess()
        assertEquals((Example(3, "amigos")), parsed)
    }
    
    @Test
    fun `generates json node equivalent to json text`() {
        val converter = converterWithMandatoryVersionField
        val original = Example(4, "seasons")
        
        val directJson =
            converter.toJson(original, testStyle)
        val jsonFromJsonNode =
            JJsonNode.toJson(converter.toJsonNode(original, NodePathRoot), testStyle)
        
        assertEquals(directJson, jsonFromJsonNode)
    }
}