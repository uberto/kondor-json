package com.ubertob.kondor.json


interface ToJson<T> {
    fun toJson(value: T): String
}

interface FromJson<T> {
    fun fromJson(json: String): JsonOutcome<T>
}

data class ToJsonF<T>(val toJson: (T) -> String) : ToJson<T> {
    override fun toJson(value: T): String = toJson(value)
    fun <U> contraTransform(f: (U) -> T): ToJson<U> = ToJsonF { toJson(f(it)) }
}

data class FromJsonF<T>(val fromJson: (String) -> JsonOutcome<T>) : FromJson<T> {
    override fun fromJson(json: String): JsonOutcome<T> = fromJson(json)
    fun <U> transform(f: (T) -> U): FromJson<U> = FromJsonF { fromJson(it).transform(f) }
}
