package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldNodeMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.outcome.Outcome

abstract class JAny<T : Any> : ObjectNodeConverterProperties<T>() {
    // If possible, use JObj that is faster at parsing and serializing.
    // This class is still useful—not just for compatibility reasons, but also because it uses JsonNode
    // as an intermediate step in parsing. This approach is slower, but necessary in cases like JSeal,
    // where all properties need to be parsed before deciding which converter to use.

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap.map, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }
}
