package com.ubertob.kondor.outcome


data class Err(override val msg: String) : OutcomeError

sealed interface Person
data class User(val name: String, val email: String): Person
data class Interested(val email: String, val question: String): Person

fun getUser(id: Int): BaseOutcome<User> =
    if (id > 0) User("user$id", "$id@example.com").asSuccess() else Err("wrong id").asFailure()

fun getMailText(name: String): BaseOutcome<String> =
    if (name.isEmpty()) Err("no name").asFailure() else "Hello $name".asSuccess()

fun sendEmailUser(email: String, text: String): UnitOutcome =
    if (text.isNotEmpty() && email.isNotEmpty()) Unit.asSuccess() else Err("empty text or email").asFailure()
