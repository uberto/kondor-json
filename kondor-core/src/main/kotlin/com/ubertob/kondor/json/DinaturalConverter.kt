package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.TokensStream

data class DinaturalConverter<T, U, JN : JsonNode>(
    private val original: JsonConverter<T, JN>,
    private val contraMap: (U) -> T,
    private val coMap: (T) -> U
) : JsonConverter<U, JN> {
    override val _nodeType = original._nodeType
    override val jsonStyle = original.jsonStyle

    override fun fromJsonNode(node: JN, path: NodePath): JsonOutcome<U> =
        original.fromJsonNode(node, path).transform(coMap)

    override fun toJsonNode(value: U): JN =
        original.toJsonNode(contraMap(value))

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<U> =
        original.fromTokens(tokens, path).transform(coMap)

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: U): CharWriter =
        original.appendValue(app, style, offset, contraMap(value))

    override fun <C, D> dimap(contraMap: (C) -> U, coMap: (U) -> D): ProfunctorConverter<C, D> =
        ProfunctorConverter(::fromJson, ::toJson).dimap(contraMap, coMap)
}