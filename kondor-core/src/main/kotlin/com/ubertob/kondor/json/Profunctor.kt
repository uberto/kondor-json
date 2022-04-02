package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeError

data class ProfunctorConverter<S, A, B, E : OutcomeError>(
    val parse: (S) -> Outcome<E, B>,
    val render: (A) -> S
): Profunctor<A,B> {
    override fun <C, D> dimap(contraFun: (C) -> A, coFun: (B) -> D): ProfunctorConverter<S, C, D, E> =
        ProfunctorConverter ({parse(it).transform(coFun)} , {render(contraFun(it))})

    @Suppress("UNCHECKED_CAST")
    override fun <C> lmap(f: (C) -> A): ProfunctorConverter<S, C, B, E> = super.lmap(f) as ProfunctorConverter<S, C, B, E>

    @Suppress("UNCHECKED_CAST")
    override fun <D> rmap(g: (B) -> D): ProfunctorConverter<S, A, D, E> = super.rmap(g) as ProfunctorConverter<S, A, D, E>
}


interface Profunctor<A, B> {
    fun <C> lmap(f: (C) -> A): Profunctor<C, B> = dimap(contraFun = f, coFun = { it })
    fun <D> rmap(g: (B) -> D): Profunctor<A, D> = dimap(contraFun = { it }, coFun = g)

    fun <C, D> dimap(contraFun: (C) -> A, coFun: (B) -> D): Profunctor<C, D>
}

typealias JsonProfunctor<T> = ProfunctorConverter<String, T, T, JsonError>

