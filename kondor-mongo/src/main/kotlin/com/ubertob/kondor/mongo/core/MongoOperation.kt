package com.ubertob.kondor.mongo.core

import com.ubertob.kondor.outcome.Outcome

typealias MongoOperation<T> = ContextReader<MongoSession, T>
typealias MongoOutcome<T> = Outcome<MongoError, T>



//!!!!!!!!!!!!!
//kondor mongo -> add joinOperation
//
//add mongo converter doc->json
//
//make the json conversion safe (from BsonDoc)


fun <U, T> mongoCalculation(calculation: MongoSession.(U) -> T): (U) -> MongoOperation<T> = //unit
    { input: U -> MongoOperation { session -> calculation(session, input) } }

fun <T> mongoOperation(operation: MongoSession.() -> T): MongoOperation<T> =
    mongoCalculation<Unit, T> { operation(this) }(Unit)

fun <T, U> MongoOperation<T>.bindCalculation(operation: MongoSession.(T) -> U): MongoOperation<U> =
    bind { input -> mongoCalculation(operation)(input) }

infix fun <T, U> MongoOperation<T>.combineWith(operation: (T) -> MongoOperation<U>): MongoOperation<U> =
    bind { input -> operation(input) }

fun <T> MongoOperation<T>.ignoreValue(): MongoOperation<Unit> =
    transform { }

operator fun <T, U, V> ((T) -> MongoOperation<U>).plus(operation: (U) -> MongoOperation<V>): (T) -> MongoOperation<V> =
    { t: T -> this(t).bind { u -> operation(u) } }

operator fun <T, U, V> ((T) -> MongoOperation<U>).plus(otherReader: MongoOperation<V>): (T) -> MongoOperation<V> =
    { t: T -> this(t).bind { otherReader } }

operator fun <T, U> MongoOperation<T>.plus(operation: (T) -> MongoOperation<U>): MongoOperation<U> =
    combineWith(operation)

operator fun <U> MongoOperation<Unit>.plus(otherReader: MongoOperation<U>): MongoOperation<U> =
    combineWith { otherReader }

infix fun <T : Any> MongoOperation<T>.exec(executor: MongoExecutor): MongoOutcome<T> = executor(this)

