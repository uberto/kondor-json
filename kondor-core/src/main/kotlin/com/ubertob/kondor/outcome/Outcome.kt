package com.ubertob.kondor.outcome

sealed class Outcome<out E : OutcomeError, out T> {

    fun <U> transform(f: (T) -> U): Outcome<E, U> =
            when (this) {
                is Success -> Success(f(this.value))
                is Failure -> this
            }

    fun <F : OutcomeError> transformFailure(f: (E) -> F): Outcome<F, T> =
            when (this) {
                is Success -> this
                is Failure -> Failure(f(this.error))
            }

    fun orThrow(): T =
            when (this) {
                is Success -> value
                is Failure -> throw OutcomeException(error)
            }

    companion object {
        inline fun <T> tryThis(block: () -> T): Outcome<ThrowableError, T> =
            try {
                block().asSuccess()
            } catch (e: Throwable) {
                ThrowableError(e).asFailure()
            }
    }
}

data class Success<T>(val value: T) : Outcome<Nothing, T>()
data class Failure<E : OutcomeError>(val error: E) : Outcome<E, Nothing>()

fun <T, U, E : OutcomeError> Outcome<E, T>.lift(f: (T) -> U): (Outcome<E, T>) -> Outcome<E, U> = { this.transform { f(it) } }

inline fun <T, U, E : OutcomeError> Outcome<E, T>.bind(f: (T) -> Outcome<E, U>): Outcome<E, U> =
        when (this) {
            is Success<T> -> f(value)
            is Failure<E> -> this
        }

inline fun <T, F : OutcomeError, E : OutcomeError> Outcome<E, T>.bindFailure(f: (E) -> Outcome<F, T>): Outcome<F, T> =
        when (this) {
            is Success<T> -> this
            is Failure<E> -> f(error)
        }

inline fun <T, E : OutcomeError> Outcome<E, T>.recover(fRec: (E) -> T): T =
        when (this) {
            is Success -> value
            is Failure -> fRec(error)
        }

inline fun <T, E : OutcomeError> Outcome<E, T>.mapNullableError(f: (T) -> E?): Outcome<E, Unit> =
        when (this) {
            is Success<T> -> {
                val error = f(this.value)
                if (error == null) Unit.asSuccess() else error.asFailure()
            }
            is Failure<E> -> this
        }

inline fun <T, E : OutcomeError> Outcome<E, T>.onFailure(block: (E) -> Nothing): T =
        when (this) {
            is Success<T> -> value
            is Failure<E> -> block(error)
        }


inline fun <T, E : OutcomeError> Outcome<E, T>.failIf(predicate: (T) -> Boolean, error: E): Outcome<E, T> =
    when (this) {
        is Success<T> -> if (predicate(value)) error.asFailure() else this
        is Failure<E> -> this
    }

fun <T : Any, E : OutcomeError> Outcome<E, T?>.failIfNull(error: E): Outcome<E, T> =
    when (this) {
        is Success -> if (value != null) value.asSuccess() else error.asFailure()
        is Failure -> this
    }

fun <E : OutcomeError, T> Iterable<Outcome<E, T>>.extract(): Outcome<E, Iterable<T>> =
    fold(emptyList<T>().asSuccess()) { acc: Outcome<E, Iterable<T>>, e: Outcome<E, T> ->
        acc.bind { list -> e.transform { list + it } }
    }

interface OutcomeError {
    val msg: String
}

data class ThrowableError(val t: Throwable) : OutcomeError {
    override val msg: String
        get() = t.message.orEmpty()
}

data class OutcomeException(val error: OutcomeError) : RuntimeException()

fun <T : OutcomeError> T.asFailure(): Outcome<T, Nothing> = Failure(this)
fun <T> T.asSuccess(): Outcome<Nothing, T> = Success(this)