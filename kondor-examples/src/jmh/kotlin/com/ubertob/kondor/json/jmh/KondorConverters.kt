package com.ubertob.kondor.json.jmh

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject


object JDemoClass : JAny<DemoClass>() {
    val text_field by str(DemoClass::text)
    val `bool-value` by bool(DemoClass::boolean)
    val intNull by num(DemoClass::nullableInt)
    val doubleNum by num(DemoClass::double)
    val list_of_strings by array(DemoClass::array)

    override fun JsonNodeObject.deserializeOrThrow() =
        DemoClass(
            text = +text_field,
            boolean = +`bool-value`,
            nullableInt = +intNull,
            double = +doubleNum,
            array = +list_of_strings
        )
}

val jDemoClasses = JList(JDemoClass)