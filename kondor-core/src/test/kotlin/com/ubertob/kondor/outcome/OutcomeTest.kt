package com.ubertob.kondor.outcome

import org.junit.jupiter.api.Test

internal class OutcomeTest {

    data class Err(override val msg: String) : OutcomeError

    data class User(val name: String, val email: String)

    fun getUser(id: Int): Outcome<OutcomeError, User> = if (id > 0) User("u$id", "$id@example.com").asSuccess() else Err("wrong id").asFailure()

    fun getMailText(name: String): Outcome<OutcomeError, String> = if (name.isEmpty()) Err("no name").asFailure() else "Hello $name".asSuccess()

    fun sendEmailUser(email: String, text: String): Outcome<OutcomeError, Unit> = if (text.isNotEmpty() && email.isNotEmpty()) Unit.asSuccess() else Err("empty text or email").asFailure()


    @Test
    fun singleBinding() {

        val res = OutcomeDo {

            sendEmailUser("123@a.com", "bye bye")
        }
    }


    @Test
    fun bindingComposition0() {

        val r =
                getUser(123)
                        .flatmap { u ->
                            getMailText(u.name)
                                    .flatmap { t ->
                                        sendEmailUser(u.email, t)
                                    }
                        }
    }

    @Test
    fun bindingComposition1() {

        val r = fun(): Outcome<OutcomeError, Unit> {

            val u = getUser(123).onFailure { return it.asFailure() }
            val t = getMailText(u.name).onFailure { return it.asFailure() }
            val e = sendEmailUser(u.email, t).onFailure { return it.asFailure() }
            return e.asSuccess()

        }


    }

    @Test
    fun bindingComposition2() {

        val res = OutcomeDo {

            val u = getUser(123)()
            val t = getMailText(u.name)()
            sendEmailUser(u.email, t)()

        }.result
    }


    @Test
    fun bindingComposition3() {

        val res = OutcomeDo {

            val u = +getUser(123)
            val t = +getMailText(u.name)
            +sendEmailUser(u.email, t)

        }.result
    }

    @Test
    fun bindingComposition4() {

        val res = OutcomeDo {

            val (u) = getUser(123)
            val (t) = getMailText(u.name)
            val (r) = sendEmailUser(u.email, t)

        }.result
    }

    fun <T : Any, U : Any, E : OutcomeError> Outcome<E, T>.flatmap(f: (T) -> Outcome<E, U>): Outcome<E, U> =
        this.bind(f)

//
//    @Test
//    fun applyComposition() {
//
//        val json= """
//            {
//            "a": 5
//            "b": "bee"
//            "c": { "name":"adam"}
//            }
//        """.trimIndent()
//        val res = json.mapTo(::Abc,
//                "a".asInt(),
//                 "b".asString(),
//                 "c".asObj<Named>()
//        )
//        }
//    }

//data class Named(val name: String)
//data class Abc(val a: Int, val b: String, val c: Named)
//
//fun <A: Any,B: Any,C: Any,D:Any> Mapper(cons: (A,B,C) ->D, p1: (String) -> Outcome<*, A>, p2: (String) -> Outcome<*, B>, p3: (String) -> Outcome<*, C> ): Outcome<OutcomeError, D> =
//       cons(p1, p2, p3)
//
//
}