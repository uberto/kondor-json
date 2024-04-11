package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.traverse
import java.io.InputStream
import java.io.OutputStream
import kotlin.text.Charsets.UTF_8

fun <T : Any> fromNdJsonToList(converter: JConverter<T>): (Sequence<String>) -> JsonOutcome<List<T>> =
    { lines ->
        lines.traverse(converter::fromJson)
    }

fun <T : Any> fromNdJson(converter: JConverter<T>): (Sequence<String>) -> Sequence<JsonOutcome<T>> =
    { lines -> lines.map(converter::fromJson) }

fun <T : Any> toNdJson(converter: JConverter<T>): (Iterable<T>) -> Sequence<String> =
    { coll ->
        coll.asSequence().map { converter.toJson(it) }
    }


fun <T : Any> fromNdJsonStream(converter: JConverter<T>): (InputStream) -> Sequence<JsonOutcome<T>> =
    { stream ->
        stream.bufferedReader().lineSequence().map { converter.fromJson(it) }
    }

fun <T : Any> toNdJsonStream(converter: JConverter<T>): (Iterable<T>, OutputStream) -> OutputStream =
    { coll, stream ->
        stream.apply {

            coll.onEach {
                val json = converter.toJson(it)
                write(json.toByteArray(UTF_8))
                write('\n'.code)
            }
        }

    }
