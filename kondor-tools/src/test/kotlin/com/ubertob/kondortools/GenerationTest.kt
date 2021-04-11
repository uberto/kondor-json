package com.ubertob.kondortools

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

data class User(val id: Int, val name: String, val isAdmin: Boolean)
enum class Roles { Admin, ReadWrite, ReadOnly }
data class Grant(val user: User, val role: Roles)

class GenerationTest {

    @Test
    fun `generate converter from User data class`() {

        val expected = """object JUser : JAny<User>() {
  private val id by num(User::id)

  private val isAdmin by bool(User::isAdmin)

  private val name by str(User::name)

  public override fun JsonNodeObject.deserializeOrThrow(): User = 
      User(
        id = +id,
        isAdmin = +isAdmin,
        name = +name
      )
}"""

        val kotlinCode = generateConverterFileFor(User::class, Grant::class)


        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")
        expectThat(kotlinCode).contains(expected)
    }


    @Test
    fun `generate converter from Grant data class`() {

        val expected = """
object JGrant : JAny<Grant>() {
  private val role by str(Grant::role)

  private val user by obj(JUser, Grant::user)

  public override fun JsonNodeObject.deserializeOrThrow(): Grant = 
      Grant(
        role = +role,
        user = +user
      )
}"""

        val kotlinCode = generateConverterFileFor(Grant::class)


        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")

        //WIP        expectThat(kotlinCode).contains(expected)
    }

    //add more tests with obj fields, array, sealed classes etc.
}