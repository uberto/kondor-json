package com.ubertob.kondor.mongo.core

import org.bson.conversions.Bson

sealed class MongoBulkOperation<T : Any> { //!!! find better name but MongoOperation is already taken... MongoWrite ?

    data class Insert<T : Any>(val document: T) : MongoBulkOperation<T>()
    data class Update<T : Any>(val filter: Bson, val update: Bson) : MongoBulkOperation<T>()
    data class Delete<T : Any>(val filter: Bson) : MongoBulkOperation<T>()
}