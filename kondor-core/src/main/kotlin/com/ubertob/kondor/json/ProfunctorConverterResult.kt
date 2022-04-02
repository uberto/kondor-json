package com.ubertob.kondor.json


fun <T,U> Result<T>.map(f: (T)->U): Result<U> = if (isSuccess) Result.success(f(getOrThrow())) else Result.failure(exceptionOrNull()!!)

data class ProfunctorConverterResult<S, A, B>(
    val parse: (S) -> Result<B>,
    val render: (A) -> S
): Profunctor<A, B> {

    override fun <C, D> dimap(contraFun: (C) -> A, g: (B) -> D): ProfunctorConverterResult<S, C, D> =
        ProfunctorConverterResult({ parse(it).map(g) }, { render(contraFun(it)) })

    @Suppress("UNCHECKED_CAST")
    override fun <C> lmap(f: (C) -> A): ProfunctorConverterResult<S, C, B> = super.lmap(f) as ProfunctorConverterResult<S, C, B>

    @Suppress("UNCHECKED_CAST")
    override fun <D> rmap(g: (B) -> D): ProfunctorConverterResult<S, A, D> = super.rmap(g) as ProfunctorConverterResult<S, A, D>
}