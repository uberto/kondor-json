package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*

abstract class JAny<T : Any> : ObjectNodeConverterProperties<T>() {

    //This class will stay both for compatibilty reasons and because it's use JsonNode as intermediate step in parsing
    //which is slower but useful in some difficult cases

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            val nodeMap = fieldValues.mapValues { value ->
                when (value) {
                    null -> JsonNodeNull
                    is String -> JsonNodeString(value)
                    is Number -> JsonNodeNumber(value)
                    is Boolean -> JsonNodeBoolean(value)
                    is JsonNode -> value
                    else -> throw JsonParsingException(
                        ConverterJsonError(
                            path,
                            "Unsupported type in JAny: ${value::class}"
                        )
                    )
                }
            }
            JsonNodeObject.buildForParsing(nodeMap, path).deserializeOrThrow() ?: error("Deserialized to null value!")
        }

}
