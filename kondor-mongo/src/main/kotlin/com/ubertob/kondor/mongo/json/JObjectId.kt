package com.ubertob.kondor.mongo.json

import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.JField
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.str
import org.bson.types.ObjectId

object JObjectId : JAny<ObjectId>() {

    val `$oid` by str(ObjectId::toHexString)
    override fun JsonNodeObject.deserializeOrThrow() =
        ObjectId(+`$oid`)

}


@JvmName("bindObjectId")
fun <PT : Any> str(binder: PT.() -> ObjectId) = JField(binder, JObjectId)
