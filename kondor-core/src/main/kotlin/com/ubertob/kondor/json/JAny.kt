package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*

abstract class JAny<T : Any> : ObjectNodeConverterProperties<T>() {

    //the idea is to leave this class with the deserializeOrThrow() method and create a new one with a different
    //way to invoke the constructor with all properties

    abstract fun JsonNodeObject.deserializeOrThrow(): T

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            val nodeMap = fieldMap.mapValues { (_, value) ->
                when (value) {
                    null -> JsonNodeNull
                    is String -> JsonNodeString(value)
                    is Number -> JsonNodeNumber(value)
                    is Boolean -> JsonNodeBoolean(value)
                    is JsonNode -> value
                    else -> throw JsonParsingException(ConverterJsonError(path, "Unsupported type: ${value::class}"))
                }
            }
            JsonNodeObject.buildForParsing(nodeMap, path).deserializeOrThrow()
                ?: throw JsonParsingException(ConverterJsonError(path, "deserializeOrThrow returned null!"))
        }

}
