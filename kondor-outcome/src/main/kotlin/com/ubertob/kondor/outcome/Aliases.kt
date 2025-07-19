package com.ubertob.kondor.outcome

/**
 * Represents an outcome that doesn't return a value (only success or failure).
 * This is a convenience alias for [BaseOutcome] with [Unit] as the success type.
 */
typealias UnitOutcome = BaseOutcome<Unit>

/**
 * Represents a standard outcome with [OutcomeError] as the error type.
 * This is a convenience alias for [Outcome] with [OutcomeError] as the error type.
 *
 * @param T The type of the success value
 */
typealias BaseOutcome<T> = Outcome<OutcomeError, T>
