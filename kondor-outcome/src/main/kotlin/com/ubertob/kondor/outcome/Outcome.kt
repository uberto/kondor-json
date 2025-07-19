package com.ubertob.kondor.outcome

/**
 * Represents the result of an operation that can either succeed with a value of type [T]
 * or fail with an error of type [E].
 *
 * This is similar to Either or Result types in other languages/libraries.
 *
 * @param E The type of error that can occur
 * @param T The type of the successful result
 */
sealed interface Outcome<out E : OutcomeError, out T> {

    /**
     * Transforms the success value using the provided function.
     *
     * @param f The transformation function to apply to the success value
     * @return A new Outcome with the transformed success value, or the original failure
     */
    fun <U> transform(f: (T) -> U): Outcome<E, U> =
        when (this) {
            is Success -> f(this.value).asSuccess()
            is Failure -> this
        }

    /**
     * Transforms the error value using the provided function.
     *
     * @param f The transformation function to apply to the error value
     * @return A new Outcome with the transformed error value, or the original success
     */
    fun <F : OutcomeError> transformFailure(f: (E) -> F): Outcome<F, T> =
        when (this) {
            is Success -> this
            is Failure -> f(this.error).asFailure()
        }

    /**
     * Extracts the success value or throws an exception if this is a failure.
     *
     * @return The success value
     * @throws OutcomeException if this is a failure
     */
    fun orThrow(): T =
        when (this) {
            is Success -> value
            is Failure -> throw OutcomeException(error)
        }

    /**
     * Extracts the success value or returns null if this is a failure.
     *
     * @return The success value or null
     */
    fun orNull(): T? = recover { null }


    companion object {

        /**
         * Transforms two outcomes using a binary function.
         * If both outcomes are successful, applies the function to their values.
         * If either outcome is a failure, returns the first failure.
         *
         * @param first The first outcome
         * @param second The second outcome
         * @param f The transformation function to apply to both success values
         * @return A new outcome with the transformed success values, or the first failure
         */
        fun <E : OutcomeError, T, U, R> transform2(
            first: Outcome<E, T>,
            second: Outcome<E, U>,
            f: (T, U) -> R
        ): Outcome<E, R> = f `!` first `*` second

        /**
         * Executes a block of code and returns its result as a success,
         * or catches any exception and returns it as a failure.
         *
         * @param block The code block to execute
         * @return A success with the block's result, or a failure with the caught exception
         */
        inline fun <T> tryOrFail(block: () -> T): Outcome<ThrowableError, T> =
            try {
                block().asSuccess()
            } catch (t: Throwable) {
                ThrowableError(t).asFailure()
            }
    }
}

/**
 * Represents a successful outcome with a value.
 *
 * @param T The type of the success value
 * @property value The success value
 */
@JvmInline
value class Success<T> internal constructor(val value: T) : Outcome<Nothing, T>

/**
 * Represents a failed outcome with an error.
 *
 * @param E The type of the error
 * @property error The error value
 */
@JvmInline
value class Failure<E : OutcomeError> internal constructor(val error: E) : Outcome<E, Nothing>

/**
 * Attempts to cast the success value to type [U], returning a failure if the cast fails.
 *
 * @param T The original type of the success value
 * @param U The target type to cast to
 * @param error A function that creates an error from the original value if the cast fails
 * @return A success with the cast value, or a failure if the cast fails
 */
inline fun <T,  reified U : T> Outcome<*, T>.castOrFail(error: (T) -> OutcomeError): Outcome<OutcomeError, U> =
    when (this) {
        is Failure -> this
        is Success -> if (value is U) {
            value.asSuccess()
        } else {
            error(value).asFailure()
        }
    }

/**
 * Attempts to cast the success value to type [U], returning a default failure if the cast fails.
 *
 * @param U The target type to cast to
 * @return A success with the cast value, or a failure with a [CastError] if the cast fails
 */
inline fun <reified U> Outcome<OutcomeError, *>.castOrFailDefault(): Outcome<OutcomeError, U> =
    when (this) {
        is Failure -> this
        is Success -> if (value is U) {
            value.asSuccess()
        } else {
            CastError(value).asFailure()
        }
    }

/**
 * Error that occurs when a cast operation fails.
 *
 * @property value The value that couldn't be cast
 */
data class CastError(val value: Any?) : OutcomeError {
    override val msg: String = "Error! invalid cast $value"
}

/**
 * Extracts the success value or recovers from a failure using the provided function.
 *
 * @param T The type of the success value
 * @param E The type of the error
 * @param recoverError A function that converts an error to a success value
 * @return The success value or the recovered value
 */
inline fun <T, E : OutcomeError> Outcome<E, T>.recover(recoverError: (E) -> T): T =
    when (this) {
        is Success -> value
        is Failure -> recoverError(error)
    }

/**
 * Applies a function to the success value that returns a new outcome.
 * This is the monadic bind operation for Outcome.
 *
 * @param T The type of the original success value
 * @param U The type of the new success value
 * @param E The type of the error
 * @param f A function that transforms the success value into a new outcome
 * @return The new outcome, or the original failure
 */
inline fun <T, U, E : OutcomeError> Outcome<E, T>.bind(f: (T) -> Outcome<E, U>): Outcome<E, U> =
    when (this) {
        is Success -> f(value)
        is Failure -> this
    }

/**
 * Applies a function to the error value that returns a new outcome.
 * This is like bind but for the error case.
 *
 * @param T The type of the success value
 * @param E The type of the original error
 * @param F The type of the new error
 * @param f A function that transforms the error into a new outcome
 * @return The new outcome, or the original success
 */
inline fun <T, E : OutcomeError, F : OutcomeError> Outcome<E, T>.bindFailure(f: (E) -> Outcome<F, T>): Outcome<F, T> =
    when (this) {
        is Success -> this
        is Failure -> f(error)
    }

/**
 * Flattens a nested outcome.
 * If this is a success containing another outcome, returns that outcome.
 * If this is a failure, returns the failure.
 *
 * @param T The type of the inner success value
 * @param E The type of the error
 * @return The flattened outcome
 */
fun <T, E : OutcomeError> Outcome<E, Outcome<E, T>>.join(): Outcome<E, T> =
    bind { it }

/**
 * Combines this outcome with another outcome into a pair.
 * If both outcomes are successful, returns a success with a pair of their values.
 * If either outcome is a failure, returns the first failure.
 *
 * @param T The type of this success value
 * @param U The type of the other success value
 * @param E The type of the error
 * @param other The other outcome to combine with
 * @return A success with a pair of values, or the first failure
 */
fun <T, U, E : OutcomeError> Outcome<E, T>.combine(other: Outcome<E, U>): Outcome<E, Pair<T, U>> =
    bind { first -> other.transform { second -> first to second } }

/**
 * Kleisli composition of two functions that return outcomes.
 * Creates a new function that applies the first function, then applies the second function to its result.
 *
 * @param A The input type of the first function
 * @param B The output type of the first function and input type of the second function
 * @param C The output type of the second function
 * @param E The type of the error
 * @param other The second function to compose with
 * @return A new function that applies both functions in sequence
 */
infix fun <A, B, C, E : OutcomeError> ((A) -> Outcome<E, B>).compose(other: (B) -> Outcome<E, C>): (A) -> Outcome<E, C> =
    { a -> this(a).bind(other) }


//convenience methods

/**
 * Applies a function to the success value but returns the original outcome.
 * If this is a success, applies the function and returns this outcome.
 * If this is a failure, returns this outcome.
 *
 * This is useful for performing side effects on the success value without changing the outcome.
 *
 * @param T The type of the success value
 * @param U The type of the function's outcome success value (ignored)
 * @param E The type of the error
 * @param f The function to apply to the success value
 * @return The original outcome
 */
fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> = when (this) {
    is Failure -> this
    is Success -> f(value).transform { value }
}

/**
 * Fails the outcome if the predicate is true for the success value.
 *
 * @param T The type of the success value
 * @param E The type of the error
 * @param predicate The predicate to check on the success value
 * @param error The function to create an error from the success value
 * @return The original outcome if the predicate is false, or a failure if the predicate is true
 */
fun <T, E : OutcomeError> Outcome<E, T>.failIf(predicate: (T) -> Boolean, error: (T) -> E): Outcome<E, T> =
    failUnless({ predicate(this).not() }, error)

/**
 * Fails the outcome unless the predicate is true for the success value.
 *
 * @param T The type of the success value
 * @param E The type of the error
 * @param predicate The predicate to check on the success value
 * @param error The function to create an error from the success value
 * @return The original outcome if the predicate is true, or a failure if the predicate is false
 */
fun <T, E : OutcomeError> Outcome<E, T>.failUnless(predicate: T.() -> Boolean, error: (T) -> E): Outcome<E, T> =
    when (this) {
        is Success -> if (predicate(value)) this else error(value).asFailure()
        is Failure -> this
    }

/**
 * Fails the outcome if the success value is null.
 *
 * @param T The type of the non-null success value
 * @param E The type of the error
 * @param error The function to create an error
 * @return A success with the non-null value, or a failure if the value is null
 */
fun <T : Any, E : OutcomeError> Outcome<E, T?>.failIfNull(error: () -> E): Outcome<E, T> =
    when (this) {
        is Success -> value?.asSuccess() ?: error().asFailure()
        is Failure -> this
    }

/**
 * Transforms the success value if it's not null.
 *
 * @param T The type of the original success value
 * @param U The type of the transformed success value
 * @param E The type of the error
 * @param f The transformation function to apply to the non-null success value
 * @return A success with the transformed value (or null), or the original failure
 */
fun <T, U, E : OutcomeError> Outcome<E, T?>.transformIfNotNull(f: (T) -> U): Outcome<E, U?> =
    transform { value ->
        value?.let(f)
    }

/**
 * Extracts the success value or calls the exit block with the error.
 *
 * @param T The type of the success value
 * @param E The type of the error
 * @param exitBlock The function to call with the error, which doesn't return
 * @return The success value
 */
inline fun <T, E : OutcomeError> Outcome<E, T>.onFailure(exitBlock: (E) -> Nothing): T =
    when (this) {
        is Success<T> -> value
        is Failure<E> -> exitBlock(error)
    }


/**
 * Interface for errors that can occur in an [Outcome].
 */
interface OutcomeError {
    /**
     * A human-readable error message.
     */
    val msg: String
}

/**
 * Exception that wraps an [OutcomeError] for use with [Outcome.orThrow].
 *
 * @property error The wrapped error
 */
data class OutcomeException(val error: OutcomeError) : RuntimeException() {
    override val message: String = error.msg
}

/**
 * Creates a failure outcome from this error.
 *
 * @param E The type of the error
 * @return A failure outcome containing this error
 */
fun <E : OutcomeError> E.asFailure(): Outcome<E, Nothing> = Failure(this)

/**
 * Creates a success outcome from this value.
 *
 * @param T The type of the value
 * @return A success outcome containing this value
 */
fun <T> T.asSuccess(): Outcome<Nothing, T> = Success(this)

/**
 * Creates a success outcome from this value if it's not null,
 * or a failure outcome with the provided error if it is null.
 *
 * @param T The type of the non-null value
 * @param E The type of the error
 * @param error The function to create an error if this value is null
 * @return A success with this value, or a failure if this value is null
 */
fun <T : Any, E : OutcomeError> T?.failIfNull(error: () -> E): Outcome<E, T> = this?.asSuccess() ?: error().asFailure()

/**
 * Creates an outcome based on a predicate.
 * If the predicate is true for this value, creates a success outcome.
 * Otherwise, creates a failure outcome using the provided error function.
 *
 * @param T The type of this value
 * @param E The type of the error
 * @param isSuccess The predicate to determine if this value is a success
 * @param error The function to create an error from this value
 * @return A success with this value, or a failure
 */
fun <T, E : OutcomeError> T.asOutcome(isSuccess: T.() -> Boolean, error: (T) -> E): Outcome<E, T> =
    if (isSuccess(this)) asSuccess() else error(this).asFailure()

/**
 * Error that wraps a [Throwable].
 *
 * @property throwable The wrapped throwable
 */
data class ThrowableError(val throwable: Throwable) : OutcomeError {
    override val msg: String = throwable.message.orEmpty()
}

/**
 * Simple error with a message.
 *
 * @property msg The error message
 */
data class MessageError(override val msg: String) : OutcomeError

/**
 * Creates a failure outcome with a [MessageError] containing this string.
 *
 * @return A failure outcome with a message error
 */
fun String.asFailure() = MessageError(this).asFailure()

/**
 * Folds this iterable into a single value, stopping at the first failure.
 * Applies the operation to each element and the accumulator, returning a failure if any operation fails.
 *
 * @param T The type of the elements in the iterable
 * @param ERR The type of the error
 * @param U The type of the accumulator and result
 * @param initial The initial value for the accumulator
 * @param operation The operation to apply to each element and the accumulator
 * @return A success with the final accumulator value, or the first failure
 */
fun <T, ERR : OutcomeError, U> Iterable<T>.foldOutcome(
    initial: U,
    operation: (acc: U, T) -> Outcome<ERR, U>
): Outcome<ERR, U> =
    fold(initial.asSuccess() as Outcome<ERR, U>) { acc, el -> acc.bind { operation(it, el) } }

/**
 * Folds this iterable into a single value with index, stopping at the first failure.
 * Applies the operation to each element, its index, and the accumulator, returning a failure if any operation fails.
 *
 * @param T The type of the elements in the iterable
 * @param ERR The type of the error
 * @param U The type of the accumulator and result
 * @param initial The initial value for the accumulator
 * @param operation The operation to apply to each element, its index, and the accumulator
 * @return A success with the final accumulator value, or the first failure
 */
fun <T, ERR : OutcomeError, U> Iterable<T>.foldOutcomeIndexed(
    initial: U,
    operation: (Int, acc: U, T) -> Outcome<ERR, U>
): Outcome<ERR, U> =
    foldIndexed(initial.asSuccess() as Outcome<ERR, U>) { index, acc, el -> acc.bind { operation(index, it, el) } }


/**
 * Applies a function to each element in this iterable, collecting the results into a list.
 * If any function application fails, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param U The type of the transformed elements
 * @param f The function to apply to each element
 * @return A success with a list of transformed elements, or the first failure
 */
fun <E : OutcomeError, T, U> Iterable<T>.traverse(f: (T) -> Outcome<E, U>): Outcome<E, List<U>> =
    foldOutcome(ArrayList(128)) { acc, e ->
        f(e).transform { acc.add(it); acc }
    }

/**
 * Applies a function to each element and its index in this iterable, collecting the results into a list.
 * If any function application fails, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param U The type of the transformed elements
 * @param f The function to apply to each element and its index
 * @return A success with a list of transformed elements, or the first failure
 */
fun <E : OutcomeError, T, U> Iterable<T>.traverseIndexed(f: (index: Int, T) -> Outcome<E, U>): Outcome<E, List<U>> =
    foldOutcomeIndexed(mutableListOf()) { index, acc, e ->
        f(index, e).transform { acc.add(it); acc }
    }

/**
 * Extracts the success values from an iterable of outcomes into a list.
 * If any outcome is a failure, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the success values
 * @return A success with a list of success values, or the first failure
 */
fun <E : OutcomeError, T> Iterable<Outcome<E, T>>.extractList(): Outcome<E, List<T>> =
    traverse { it }


/**
 * Applies a function to each element in this iterable, collecting the results into a set.
 * If any function application fails, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param U The type of the transformed elements
 * @param f The function to apply to each element
 * @return A success with a set of transformed elements, or the first failure
 */
fun <E : OutcomeError, T, U> Iterable<T>.traverseToSet(f: (T) -> Outcome<E, U>): Outcome<E, Set<U>> =
    foldOutcome(HashSet(128)) { acc, e ->
        f(e).transform { acc.add(it); acc }
    }

/**
 * Extracts the success values from an iterable of outcomes into a set.
 * If any outcome is a failure, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the success values
 * @return A success with a set of success values, or the first failure
 */
fun <E : OutcomeError, T> Iterable<Outcome<E, T>>.extractSet(): Outcome<E, Set<T>> =
    traverseToSet { it }

/**
 * Applies a function to each element in this sequence, collecting the results into a list.
 * If any function application fails, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the elements in the sequence
 * @param U The type of the transformed elements
 * @param f The function to apply to each element
 * @return A success with a list of transformed elements, or the first failure
 */
fun <E : OutcomeError, T, U> Sequence<T>.traverse(f: (T) -> Outcome<E, U>): Outcome<E, List<U>> =
    foldOutcome(ArrayList(128)) { acc, e ->
        f(e).transform { acc.add(it); acc }
    }

/**
 * Extracts the success values from a sequence of outcomes into a list.
 * If any outcome is a failure, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the success values
 * @return A success with a list of success values, or the first failure
 */
fun <E : OutcomeError, T> Sequence<Outcome<E, T>>.extractList(): Outcome<E, List<T>> =
    traverse { it }

/**
 * Applies a function to each element in this sequence, collecting the results into a set.
 * If any function application fails, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the elements in the sequence
 * @param U The type of the transformed elements
 * @param f The function to apply to each element
 * @return A success with a set of transformed elements, or the first failure
 */
fun <E : OutcomeError, T, U> Sequence<T>.traverseToSet(f: (T) -> Outcome<E, U>): Outcome<E, Set<U>> =
    foldOutcome(HashSet(128)) { acc, e ->
        f(e).transform { acc.add(it); acc }
    }

/**
 * Extracts the success values from a sequence of outcomes into a set.
 * If any outcome is a failure, returns the first failure.
 *
 * @param E The type of the error
 * @param T The type of the success values
 * @return A success with a set of success values, or the first failure
 */
fun <E : OutcomeError, T> Sequence<Outcome<E, T>>.extractSet(): Outcome<E, Set<T>> =
    traverseToSet { it }


/**
 * Folds this sequence into a single value, stopping at the first failure.
 * Applies the operation to each element and the accumulator, returning a failure if any operation fails.
 *
 * This implementation uses tail recursion for efficiency.
 *
 * @param T The type of the elements in the sequence
 * @param ERR The type of the error
 * @param U The type of the accumulator and result
 * @param initial The initial value for the accumulator
 * @param operation The operation to apply to each element and the accumulator
 * @return A success with the final accumulator value, or the first failure
 */
fun <T, ERR : OutcomeError, U> Sequence<T>.foldOutcome(
    initial: U,
    operation: (acc: U, T) -> Outcome<ERR, U>
): Outcome<ERR, U> {

    val iter = iterator()

    tailrec fun loop(acc: U): Outcome<ERR, U> =
        if (!iter.hasNext()) acc.asSuccess()
        else when (val el = operation(acc, iter.next())) {
            is Failure -> el
            is Success -> loop(el.value)
        }

    return loop(initial)
}


/**
 * Applies a binary function to an outcome, returning a function that takes the second argument.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param D The return type of the function
 * @param ER The type of the error
 * @param other The outcome containing the first argument
 * @return An outcome containing a function that takes the second argument
 */
infix fun <A, B, D, ER : OutcomeError> ((A, B) -> D).`!`(other: Outcome<ER, A>): Outcome<ER, (B) -> D> =
    other.transform { a -> { b -> this(a, b) } }

/**
 * Applies a ternary function to an outcome, returning a function that takes the second and third arguments.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param C The type of the third argument
 * @param D The return type of the function
 * @param ER The type of the error
 * @param other The outcome containing the first argument
 * @return An outcome containing a function that takes the second and third arguments
 */
infix fun <A, B, C, D, ER : OutcomeError> ((A, B, C) -> D).`!`(other: Outcome<ER, A>): Outcome<ER, (B) -> (C) -> D> =
    other.transform { a -> { b -> { c -> this(a, b, c) } } }

/**
 * Applies a quaternary function to an outcome, returning a function that takes the second, third, and fourth arguments.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param C The type of the third argument
 * @param D The type of the fourth argument
 * @param E The return type of the function
 * @param ER The type of the error
 * @param other The outcome containing the first argument
 * @return An outcome containing a function that takes the second, third, and fourth arguments
 */
infix fun <A, B, C, D, E, ER : OutcomeError> ((A, B, C, D) -> E).`!`(other: Outcome<ER, A>): Outcome<ER, (B) -> (C) -> (D) -> E> =
    other.transform { a -> { b -> { c -> { d -> this(a, b, c, d) } } } }

/**
 * Applies a quinary function to an outcome, returning a function that takes the second, third, fourth, and fifth arguments.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param C The type of the third argument
 * @param D The type of the fourth argument
 * @param E The type of the fifth argument
 * @param F The return type of the function
 * @param ER The type of the error
 * @param other The outcome containing the first argument
 * @return An outcome containing a function that takes the second, third, fourth, and fifth arguments
 */
infix fun <A, B, C, D, E, F, ER : OutcomeError> ((A, B, C, D, E) -> F).`!`(other: Outcome<ER, A>): Outcome<ER, (B) -> (C) -> (D) -> (E) -> F> =
    other.transform { a -> { b -> { c -> { d -> { e -> this(a, b, c, d, e) } } } } }

/**
 * Applies a function in an outcome to an argument in another outcome.
 * This is the applicative functor application for Outcome.
 *
 * @param A The type of the argument
 * @param B The return type of the function
 * @param ER The type of the error
 * @param a The outcome containing the argument
 * @return An outcome containing the result of applying the function to the argument
 */
@Suppress("DANGEROUS_CHARACTERS")
infix fun <A, B, ER : OutcomeError> Outcome<ER, (A) -> B>.`*`(a: Outcome<ER, A>): Outcome<ER, B> =
    bind { a.transform(it) }

/**
 * Applies a binary function to an outcome from a function, returning a function that takes the second argument.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param D The return type of the function
 * @param ER The type of the error
 * @param other The function that returns an outcome containing the first argument
 * @return An outcome containing a function that takes the second argument
 */
infix fun <A, B, D, ER : OutcomeError> ((A, B) -> D).`!`(other: () -> Outcome<ER, A>): Outcome<ER, (B) -> D> =
    other().transform { a -> { b -> this(a, b) } }

/**
 * Applies a ternary function to an outcome from a function, returning a function that takes the second and third arguments.
 * This is part of the applicative functor implementation for Outcome.
 *
 * @param A The type of the first argument
 * @param B The type of the second argument
 * @param C The type of the third argument
 * @param D The return type of the function
 * @param ER The type of the error
 * @param other The function that returns an outcome containing the first argument
 * @return An outcome containing a function that takes the second and third arguments
 */
infix fun <A, B, C, D, ER : OutcomeError> ((A, B, C) -> D).`!`(other: () -> Outcome<ER, A>): Outcome<ER, (B) -> (C) -> D> =
    other().transform { a -> { b -> { c -> this(a, b, c) } } }

/**
 * Applies a function in an outcome to an argument in an outcome from a function.
 * This is the applicative functor application for Outcome.
 *
 * @param A The type of the argument
 * @param B The return type of the function
 * @param ER The type of the error
 * @param a The function that returns an outcome containing the argument
 * @return An outcome containing the result of applying the function to the argument
 */
@Suppress("DANGEROUS_CHARACTERS")
infix fun <A, B, ER : OutcomeError> Outcome<ER, (A) -> B>.`*`(a: () -> Outcome<ER, A>): Outcome<ER, B> =
    bind { a().transform(it) }


//for side effects

/**
 * Executes a side effect on the success value and returns the original outcome.
 *
 * @param E The type of the error
 * @param T The type of the success value
 * @param block The side effect to execute on the success value
 * @return The original outcome
 */
fun <E : OutcomeError, T> Outcome<E, T>.withSuccess(block: (T) -> Unit): Outcome<E, T> =
    transform { it.also(block) }

/**
 * Executes a side effect on the error value and returns the original outcome.
 *
 * @param E The type of the error
 * @param T The type of the success value
 * @param block The side effect to execute on the error value
 * @return The original outcome
 */
fun <E : OutcomeError, T> Outcome<E, T>.withFailure(block: (E) -> Unit): Outcome<E, T> =
    transformFailure { it.also(block) }

/**
 * Applies a function to the success value but returns the original outcome.
 * Similar to [bindAndIgnore] but with a different implementation.
 *
 * @param E The type of the error
 * @param T The type of the success value
 * @param U The type of the function's outcome success value (ignored)
 * @param f The function to apply to the success value
 * @return The original outcome
 */
fun <E : OutcomeError, T, U> Outcome<E, T>.bindAlso(f: (T) -> Outcome<E, U>): Outcome<E, T> =
    bind { value -> f(value).transform { value } }

//for operating with collections inside Outcome

/**
 * Maps each element in an iterable inside a success outcome.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param U The type of the mapped elements
 * @param f The mapping function to apply to each element
 * @return A success with a list of mapped elements, or the original failure
 */
fun <E : OutcomeError, T, U> Outcome<E, Iterable<T>>.map(f: (T) -> U): Outcome<E, List<U>> =
    transform { it.map(f) }

/**
 * Filters elements in an iterable inside a success outcome.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param f The predicate to filter elements
 * @return A success with a list of filtered elements, or the original failure
 */
fun <E : OutcomeError, T> Outcome<E, Iterable<T>>.filter(f: (T) -> Boolean): Outcome<E, List<T>> =
    transform { it.filter(f) }

/**
 * Maps each element in an iterable inside a success outcome to an outcome, and flattens the results.
 *
 * @param E The type of the error
 * @param T The type of the elements in the iterable
 * @param U The type of the mapped elements
 * @param f The mapping function that returns an outcome for each element
 * @return A success with a list of mapped elements, or the first failure
 */
fun <E : OutcomeError, T, U> Outcome<E, Iterable<T>>.flatMap(f: (T) -> Outcome<E, U>): Outcome<E, List<U>> =
    bind { it.traverse(f) }
