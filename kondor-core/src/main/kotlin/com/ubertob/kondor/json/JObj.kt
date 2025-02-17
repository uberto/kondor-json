package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.bind

abstract class JObj<T : Any> : ObjectNodeConverterProperties<T>() {
    //this is the new JAny

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p -> parseFields(t, p, ::parseField) },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fieldMap ->
                tryFromNode(path) {
                    deserFieldMapOrThrow(fieldMap)
                }
            }




    override fun JsonNodeObject.deserializeOrThrow(): T? = error("Deprecated use the new deserFieldMapOrThrow")

    abstract fun deserFieldMapOrThrow(fieldMap: FieldMap): T //this is the method that concrete converter will have to implement
}