package com.ubertob.kondor.json.jmh

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.array.JList
import com.ubertob.kondor.json.jsonnode.JsonNodeObject


object JDemoClass : JAny<DemoClass>() {
    val text by str(DemoClass::text)
    val boolean by bool(DemoClass::boolean)
    val nullableInt by num(DemoClass::nullableInt)
    val float by num(DemoClass::float)
    val double by num(DemoClass::double)
    val array by array(DemoClass::array)

    override fun JsonNodeObject.deserializeOrThrow() =
        DemoClass(
            text = +text,
            boolean = +boolean,
            nullableInt = +nullableInt,
            float = +float,
            double = +double,
            array = +array
        )
}

val jDemoClasses = JList(JDemoClass)

val demoClassNdProducer = toNdJson(JDemoClass)
val demoClassNdConsumer = fromNdJsonToList(JDemoClass)
