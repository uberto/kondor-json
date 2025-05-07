package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.bind

abstract class JObj<T : Any> : ObjectNodeConverterProperties<T>() {
    //this is the new JAny with faster parsing !!!

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p -> parseFields(t, p, ::parseField) },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fieldMap ->
                fromFieldValues(fieldMap, path)
            }

    override fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            fieldValues.deserializeOrThrow(path)
        }

    abstract fun FieldsValues.deserializeOrThrow(path: NodePath): T //this is the method that concrete converter will have to implement
}
