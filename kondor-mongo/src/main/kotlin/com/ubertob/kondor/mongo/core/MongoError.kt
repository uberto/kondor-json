package com.ubertob.kondor.mongo.core

import com.ubertob.kondor.outcome.OutcomeError

sealed class MongoError : OutcomeError
data class MongoErrorException(val clusterDesc: String, val databaseName: String, val e: Exception) :
    MongoError() {
    override val msg: String = "$e - dbname:$databaseName instance:$clusterDesc"
}

data class MongoConversionError(override val msg: String) : MongoError()

data class MongoErrorInternal(val connection: MongoConnection, val databaseName: String, val errorDesc: String) :
    MongoError() {
    override val msg: String = "$errorDesc - dbname:$databaseName instance:${connection.connString}"
}