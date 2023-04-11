package com.gamasoft.kondor.mongo.core

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError

data class ContextReader<CTX, out T>(val runWith: (CTX) -> T) {

    fun <U> transform(f: (T) -> U): ContextReader<CTX, U> = ContextReader { ctx -> f(runWith(ctx)) }

    fun withAction(f: (T) -> Unit): ContextReader<CTX, Unit> = transform(f)

    fun <U> bind(f: (T) -> ContextReader<CTX, U>): ContextReader<CTX, U> =
        ContextReader { ctx -> f(runWith(ctx)).runWith(ctx) }


}

typealias KArrow<A, B, CTX> = (A) -> ContextReader<CTX, B>

infix fun <A, B, C, CTX> KArrow<A, B, CTX>.composeWith(other: KArrow<B, C, CTX>): KArrow<A, C, CTX> =
    { a -> this(a).bind { b -> other(b) } }

infix fun <A, B, C, CTX> KArrow<A, B, CTX>.composeWith(other: ContextReader<CTX, C>): KArrow<A, C, CTX> =
    composeWith { other }

infix fun <CTX, T> ContextReader<CTX, T>.composeWith(other: ContextReader<CTX, T>): ContextReader<CTX, T> =
    bind { other }

fun <CTX, T> ContextReader<CTX, ContextReader<CTX, T>>.join(): ContextReader<CTX, T> =
    bind { it }


interface ContextProvider<CTX> {
    operator fun <T> invoke(context: ContextReader<CTX, T>): Outcome<OutcomeError, T>
}