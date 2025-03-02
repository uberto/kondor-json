package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.bind

abstract class JObj<T : Any> : JAny<T>() {
    //this is the new JAny

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p -> parseFields(t, p, ::parseField) },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fieldMap ->
                fromFieldMap(fieldMap, path)
            }

    override fun fromFieldMap(fieldMap: Map<String, Any?>, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            deserFieldMapOrThrow(fieldMap)
        }

    override fun JsonNodeObject.deserializeOrThrow(): T =
        deserFieldMapOrThrow(_fieldMap)

    abstract fun deserFieldMapOrThrow(fieldMap: FieldMap): T //this is the method that concrete converter will have to implement
}
