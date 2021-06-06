package com.ubertob.kondor.json

import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.json.datetime.localDate
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate

class JLocalDatesTest {
    @Test
    fun `reads an object with date in a custom format`() {
        val json =
            // language=json
            """
            {
              "id": "abcd",
              "date": "22/05/2021"
            }
        """.trimIndent()

        val result = JTransaction.fromJson(json)

        expectThat(result.expectSuccess().date).isEqualTo(LocalDate.of(2021, 5, 22))
    }

    data class Transaction(val id: String, val date: LocalDate)

    object JTransaction : JAny<Transaction>() {
        private val id by str(Transaction::id)
        private val date by localDate("dd/MM/yyyy", Transaction::date)

        override fun JsonNodeObject.deserializeOrThrow(): Transaction =
            Transaction(
                id = +id,
                date = +date,
            )
    }
}

