package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError

data class ProfunctorConverter<S, A, B, E : OutcomeError>(
    val parse: (S) -> Outcome<E, B>,
    val render: (A) -> S
): Profunctor<A,B> {
    override fun <C, D> dimap(f: (C) -> A, g: (B) -> D): ProfunctorConverter<S, C, D, E> =
        ProfunctorConverter ({parse(it).transform(g)} , {render(f(it))})
}


interface Profunctor<A, B> {
    fun <C> lmap(f: (C) -> A): Profunctor<C, B> = dimap(f, { it })
    fun <D> rmap(g: (B) -> D): Profunctor<A, D> = dimap({ it }, g)

    fun <C, D> dimap(f: (C) -> A, g: (B) -> D): Profunctor<C, D>
}

typealias JsonProfunctor<T> = ProfunctorConverter<String, T, T, JsonError>

