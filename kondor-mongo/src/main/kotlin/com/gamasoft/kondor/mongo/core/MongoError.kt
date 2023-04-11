package com.gamasoft.kondor.mongo.core

import com.ubertob.kondor.outcome.OutcomeError

sealed class MongoError : OutcomeError
data class MongoErrorException(val connection: MongoConnection, val databaseName: String, val e: Exception) :
    MongoError() {
    override val msg: String = "$e - dbname:$databaseName instance:${connection.connString}"
}

data class MongoErrorInternal(val connection: MongoConnection, val databaseName: String, val errorDesc: String) :
    MongoError() {
    override val msg: String = "$errorDesc - dbname:$databaseName instance:${connection.connString}"
}