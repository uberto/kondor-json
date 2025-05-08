package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldNodeMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.outcome.Outcome

abstract class JAny<T : Any> : ObjectNodeConverterProperties<T>() {

    //This class will stay both for compatibilty reasons and because it's use JsonNode as intermediate step in parsing
    //which is slower but useful in some difficult cases

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap.map, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }
}
