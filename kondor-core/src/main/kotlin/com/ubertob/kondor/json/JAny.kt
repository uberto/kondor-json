package com.ubertob.kondor.json

abstract class JAny<T : Any> : ObjectNodeConverterProperties<T>() {

    //the idea is to leave this class with the deserializeOrThrow() method and create a new one with a different
    //way to invoke the constructor with all properties

}
