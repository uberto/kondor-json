package com.ubertob.kondor.outcome

class OutcomeDo<T : Any>(val f: OutcomeDo<*>.() -> T) {

    operator fun <E : OutcomeError, T : Any> Outcome<E, T>.unaryPlus(): T =
            when (this) {
                is Success -> value
                is Failure -> throw WithMonadsException(error)
            }

    operator fun <E : OutcomeError, T : Any> Outcome<E, T>.invoke(): T =
            when (this) {
                is Success -> value
                is Failure -> throw WithMonadsException(error)
            }

    operator fun <E : OutcomeError, T : Any> Outcome<E, T>.component1(): T =
            when (this) {
                is Success -> value
                is Failure -> throw WithMonadsException(error)
            }


    data class WithMonadsException(val error: OutcomeError) : Exception()

    val result: Outcome<OutcomeError, T>
        get() =
            try {
                f().asSuccess()
            } catch (e: WithMonadsException) {
                 e.error.asFailure()
            }

}

