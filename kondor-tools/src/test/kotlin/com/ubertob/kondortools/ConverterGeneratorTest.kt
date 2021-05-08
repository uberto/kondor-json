package com.ubertob.kondortools

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

data class User(val id: Int, val name: String, val isAdmin: Boolean)
enum class Roles { Admin, ReadWrite, ReadOnly }
data class Grant(val user: User, val role: Roles)
data class App(val name: String, val users: Set<User>)

class ConverterGeneratorTest {

    @Test
    fun `generate converter from User data class`() {

        val expected = """import com.ubertob.kondor.json.*
import com.ubertob.kondortools.User

object JUser : JAny<User>() {
  private val id by num(User::id)

  private val isAdmin by bool(User::isAdmin)

  private val name by str(User::name)

  override fun JsonNodeObject.deserializeOrThrow(): User = 
      User(
        id = +id,
        isAdmin = +isAdmin,
        name = +name
      )
}
"""

        val kotlinCode = generateConverterFileFor(User::class)

        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")
        expectThat(kotlinCode).isEqualTo(expected)
    }


    @Test
    fun `generate converter from Grant data class`() {

        val expected = """import com.ubertob.kondor.json.*
import com.ubertob.kondortools.Grant

object JGrant : JAny<Grant>() {
  private val role by str(Grant::role)

  private val user by obj(JUser, Grant::user)

  override fun JsonNodeObject.deserializeOrThrow(): Grant = 
      Grant(
        role = +role,
        user = +user
      )
}
"""

        val kotlinCode = generateConverterFileFor(Grant::class)


        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")

        expectThat(kotlinCode).isEqualTo(expected)
    }


    @Test
    fun `generate converter from App data class`() {

        val expected = """import com.ubertob.kondor.json.*
import com.ubertob.kondortools.App

object JApp : JAny<App>() {
  private val name by str(App::name)

  private val users by array(JUser, App::users)

  override fun JsonNodeObject.deserializeOrThrow(): App = 
      App(
        name = +name,
        users = +users
      )
}
"""

        val kotlinCode = generateConverterFileFor(App::class)


        println("identical: ${kotlinCode == expected}")
        println("generated:\n$kotlinCode")

        expectThat(kotlinCode).isEqualTo(expected)
    }

    //add more tests with obj fields, array, sealed classes etc.
}