package com.ubertob.kondor.json

import com.ubertob.kondor.json.datetime.JLocalDate
import com.ubertob.kondor.json.datetime.str
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JLocalDateTest {
    @Test
    fun `Json LocalDate with custom format`() {
        val date = LocalDate.of(2020, 10, 15)
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val jLocalDate = JLocalDate.withFormatter(format)

        val dateAsdJsonNode = JsonNodeString("15/10/2020")
        expectThat(jLocalDate.toJsonNode(date)).isEqualTo(dateAsdJsonNode)
        expectThat(jLocalDate.fromJsonNode(dateAsdJsonNode, NodePathRoot).expectSuccess()).isEqualTo(date)

        val dateAsString = "\"15/10/2020\""
        expectThat(jLocalDate.toJson(date)).isEqualTo(dateAsString)
        expectThat(jLocalDate.fromJson(dateAsString).expectSuccess()).isEqualTo(date)
    }

    @Test
    fun `Json LocalDate with custom format as String`() {
        val date = LocalDate.of(2021, 1, 6)
        val format = "dd-MM-yyyy"
        val jLocalDate = JLocalDate.withPattern(format)

        val dateAsJsonNode = JsonNodeString("06-01-2021")
        expectThat(jLocalDate.toJsonNode(date)).isEqualTo(dateAsJsonNode)
        expectThat(jLocalDate.fromJsonNode(dateAsJsonNode, NodePathRoot).expectSuccess()).isEqualTo(date)

        val dateAsString = "\"06-01-2021\""
        expectThat(jLocalDate.toJson(date)).isEqualTo(dateAsString)
        expectThat(jLocalDate.fromJson(dateAsString).expectSuccess()).isEqualTo(date)
    }

    @Nested
    inner class ShortFunctions {
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

        @Nested
        @DisplayName("reads an object with optional date")
        inner class OptionalDate {
            @Test
            fun `date missing`() {
                val json =
                    // language=json
                    """
                        {
                          "id": "abcd"
                        }
                    """.trimIndent()

                val result = JTransactionWithOptionalDate.fromJson(json)

                expectThat(result.expectSuccess().date).isEqualTo(null)
            }

            @Test
            fun `date present`() {
                val json =
                    // language=json
                    """
                    {
                      "id": "abcd",
                      "date": "01-02-2021"
                    }
                """.trimIndent()

                val result = JTransactionWithOptionalDate.fromJson(json)

                expectThat(result.expectSuccess().date).isEqualTo(LocalDate.of(2021, 2, 1))
            }
        }

    }

    data class Transaction(val id: String, val date: LocalDate)

    object JTransaction : JAny<Transaction>() {
        private val id by str(Transaction::id)
        private val date by str("dd/MM/yyyy", Transaction::date)

        override fun JsonNodeObject.deserializeOrThrow(): Transaction =
            Transaction(
                id = +id,
                date = +date,
            )
    }

    data class TransactionWithOptionalDate(val id: String, val date: LocalDate?)

    object JTransactionWithOptionalDate : JAny<TransactionWithOptionalDate>() {
        private val id by str(TransactionWithOptionalDate::id)
        private val date by str("dd-MM-yyyy", TransactionWithOptionalDate::date)

        override fun JsonNodeObject.deserializeOrThrow(): TransactionWithOptionalDate =
            TransactionWithOptionalDate(
                id = +id,
                date = +date,
            )
    }
}

