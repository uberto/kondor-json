package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject

abstract class JObj<T : Any> : ObjectNodeConverterProperties<T>() {

    //this is the new JAny

    override fun JsonNodeObject.deserializeOrThrow(): T? = error("Deprecated use the new deserFieldMapOrThrow")

    abstract fun deserFieldMapOrThrow(fieldMap: FieldMap): T //this is the method that concrete converter will have to implement
}