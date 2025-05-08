package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.FieldNodeMap
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.traverse

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

    fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            fieldValues.deserializeOrThrow(path)
        }

    override fun fromFieldNodeMap(fieldNodeMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        getProperties().traverse { property ->
            property.getter(fieldNodeMap, path)
                .transform { property.propName to it }
        }.bind {
            fromFieldValues(FieldMap(it.toMap()), path)
        }

    abstract fun FieldsValues.deserializeOrThrow(path: NodePath): T //this is the method that concrete converter will have to implement
}
