package com.ubertob.kondortools

import org.junit.jupiter.api.Test

data class User(val id: Int, val name: String, val isAdmin: Boolean)

class GenerationTest {

    //    @Disabled("WIP")
    @Test
    fun `generate converter from Person data class`() {

        val expected = """
            object JUser : JAny<User>() {

                private val id by num(User::id)
                private val name by str(User::name)
                private val isAdmin by bool(User::isAdmin)

                override fun JsonNodeObject.deserializeOrThrow() =
                    User(
                        id = +id,
                        name = +name,
                        isAdmin = +isAdmin
                    )
            }
        """.trimIndent()

        val kotlinCode = generateConverterFileFor(User::class)

        //still wip     expectThat(kotlinCode).isEqualTo(expected)

        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")
    }
}