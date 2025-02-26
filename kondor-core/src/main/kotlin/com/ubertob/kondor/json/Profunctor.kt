package com.ubertob.kondor.json

interface Profunctor<A, B> {
    fun <S> lmap(f: (S) -> A): Profunctor<S, B> = dimap(contraMap = f, coMap = { it })
    fun <T> rmap(g: (B) -> T): Profunctor<A, T> = dimap(contraMap = { it }, coMap = g)

    fun <S, T> dimap(contraMap: (S) -> A, coMap: (B) -> T): Profunctor<S, T>
}

data class ProfunctorConverter<A, B>(
    private val parse: (String) -> JsonOutcome<B>,
    private val render: (A) -> String
) : Profunctor<A, B>, FromJson<B>, ToJson<A> {
    override fun <C, D> dimap(contraMap: (C) -> A, coMap: (B) -> D): ProfunctorConverter<C, D> =
        ProfunctorConverter({ parse(it).transform(coMap) }, { render(contraMap(it)) })


    @Suppress("UNCHECKED_CAST")
    override fun <C> lmap(f: (C) -> A): ProfunctorConverter<C, B> = super.lmap(f) as ProfunctorConverter<C, B>

    @Suppress("UNCHECKED_CAST")
    override fun <D> rmap(g: (B) -> D): ProfunctorConverter<A, D> = super.rmap(g) as ProfunctorConverter<A, D>

    override fun toJson(value: A) = render(value)
    override fun fromJson(json: String) = parse(json)
}
