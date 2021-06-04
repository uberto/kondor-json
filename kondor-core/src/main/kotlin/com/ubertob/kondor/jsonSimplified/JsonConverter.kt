package com.ubertob.kondor.jsonSimplified

import com.ubertob.kondor.outcome.*


data class JsonError(val reason: String) : OutcomeError {
    override val msg = "Json error! $reason"

    override fun toString(): String = msg
}

typealias JsonOutcome<T> = Outcome<JsonError, T>

typealias JConverter<T> = JsonConverter<T, *>

typealias JArrayConverter<CT> = JsonConverter<CT, JsonNodeArray>

interface JsonConverter<T, JN : JsonNode>: Profunctor<T, T>  {

    val parse: TokensStream.() -> JsonOutcome<JN>

    @Suppress("UNCHECKED_CAST") //but we are confident it's safe
    private fun safeCast(node: JsonNode): JsonOutcome<JN?> = (node as JN).asSuccess()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T?> = safeCast(node).bind(::fromJsonNodeNull)

    fun fromJsonNode(node: JN): JsonOutcome<T>

    fun fromJsonNodeNull(node: JN?): JsonOutcome<T?> = node?.let { fromJsonNode(it) } ?: null.asSuccess()

    fun toJsonNode(value: T): JN

    fun toJson(value: T): String = toJsonNode(value).render()

    fun fromJson(jsonString: String): JsonOutcome<T> =
        JsonLexer.tokenize(jsonString).run {
            parse(this)
                .bind { fromJsonNode(it) }
                .bind {
                    if (hasNext())
                        parsingFailure("EOF", next())
                    else
                        it.asSuccess()
                }
        }

    override fun <C, D> dimap(f: (C) -> T, g: (T) -> D): ProfunctorConverter<String, C, D, JsonError> = ProfunctorConverter(::fromJson, ::toJson).dimap(f,g)

    override fun <C> lmap(f: (C) -> T): ProfunctorConverter<String, C, T, JsonError> = ProfunctorConverter(::fromJson, ::toJson).lmap(f)

    override fun <D> rmap(g: (T) -> D): ProfunctorConverter<String, T, D, JsonError> = ProfunctorConverter(::fromJson, ::toJson).rmap(g)

}





