package com.ubertob.kondor.json.array

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues

class JAnyAsArray<T : Any, PT : Any, IterT : Iterable<T>>(
    override val converter: JConverter<T>,
    override val cons: (IterT) -> PT,
    override val binder: PT.() -> IterT,
) : JArrayRepresentable<PT, T, IterT>() {

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: PT): CharWriter {
        return app.appendArrayValues(style, offset, value.binder(), converter::appendValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun convertToAny(from: Iterable<T?>): PT = cons(from.filterNotNull() as IterT)
}

