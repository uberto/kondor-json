package com.ubertob.kondor.mongo.core

import com.mongodb.client.model.Filters
import com.ubertob.kondor.json.JsonProperty
import org.bson.conversions.Bson

object MongoSpecialFields {
    const val oid = "\$oid"
    const val date = "\$date"
}


infix fun <T : CharSequence> JsonProperty<T>.eq(value: T): Bson =
    Filters.eq(propName, value)

infix fun <T : Number> JsonProperty<T>.eq(value: T): Bson =
    Filters.eq(propName, value)

infix fun JsonProperty<Boolean>.eq(value: Boolean): Bson =
    Filters.eq(propName, value)

infix fun <T : Comparable<T>> JsonProperty<T>.lt(value: T): Bson =
    Filters.lt(propName, value)

infix fun <T : Comparable<T>> JsonProperty<T>.lte(value: T): Bson =
    Filters.lte(propName, value)

infix fun <T : Comparable<T>> JsonProperty<T>.gt(value: T): Bson =
    Filters.gt(propName, value)

infix fun <T : Comparable<T>> JsonProperty<T>.gte(value: T): Bson =
    Filters.gte(propName, value)


infix fun JsonProperty<*>.size(value: Int): Bson =
    Filters.size(propName, value)

infix fun <T> JsonProperty<T>.all(values: Iterable<T>): Bson =
    Filters.all(propName, values)

infix fun <T> JsonProperty<T>.`in`(values: Iterable<T>): Bson =
    Filters.`in`(propName, values)
