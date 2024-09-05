package com.ubertob.kondor.mongo.core

import org.bson.conversions.Bson

sealed class WriteOperation<T : Any> {

    data class Insert<T : Any>(val document: T) : WriteOperation<T>()
    data class Update<T : Any>(val filter: Bson, val update: Bson) : WriteOperation<T>()
    data class Delete<T : Any>(val filter: Bson) : WriteOperation<T>()
}