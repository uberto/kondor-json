package com.ubertob.kondor.json

import com.ubertob.kondor.json.datetime.JLocalDateTime
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
import java.text.DateFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class JLocalDateTimeTest {
    
    private val am = DateFormatSymbols(Locale.ENGLISH).amPmStrings[0]
    private val pm = DateFormatSymbols(Locale.ENGLISH).amPmStrings[1]
    
    @Test
    fun `Json LocalDateTime with custom format`() {
        val date = LocalDateTime.of(2020, 10, 15, 14, 1, 34)
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a").withLocale(Locale.ENGLISH)
        val jLocalDateTime = JLocalDateTime.withFormatter(format)

        val dateTimeAsJsonNode = JsonNodeString("15/10/2020 02:01:34 $pm")
        expectThat(jLocalDateTime.toJsonNode(date)).isEqualTo(dateTimeAsJsonNode)
        expectThat(jLocalDateTime.fromJsonNode(dateTimeAsJsonNode, NodePathRoot).expectSuccess()).isEqualTo(date)

        val dateTimeAsString = "\"15/10/2020 02:01:34 $pm\""
        expectThat(jLocalDateTime.toJson(date)).isEqualTo(dateTimeAsString)
        expectThat(jLocalDateTime.fromJson(dateTimeAsString).expectSuccess()).isEqualTo(date)
    }

    @Test
    fun `Json LocalDateTime with custom format as String`() {
        val dateTime = LocalDateTime.of(2020, 10, 15, 14, 1, 34)
        val jLocalDateTime = JLocalDateTime.withPattern("dd/MM/yyyy hh:mm:ss a")

        val dateTimeAsJsonNode = JsonNodeString("15/10/2020 02:01:34 $pm")
        expectThat(jLocalDateTime.toJsonNode(dateTime)).isEqualTo(dateTimeAsJsonNode)
        expectThat(jLocalDateTime.fromJsonNode(dateTimeAsJsonNode, NodePathRoot).expectSuccess()).isEqualTo(dateTime)

        val dateTimeAsString = "\"15/10/2020 02:01:34 $pm\""
        expectThat(jLocalDateTime.toJson(dateTime)).isEqualTo(dateTimeAsString)
        expectThat(jLocalDateTime.fromJson(dateTimeAsString).expectSuccess()).isEqualTo(dateTime)
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
                      "date": "15/10/2020 02:01:34 $pm"
                    }
                """.trimIndent()

            val result = JTransaction.fromJson(json)

            expectThat(result.expectSuccess().dateTime).isEqualTo(LocalDateTime.of(2020, 10, 15, 14, 1, 34))
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
                      "date": "01/02/2021 02:15:56 $am"
                    }
                """.trimIndent()

                val result = JTransactionWithOptionalDate.fromJson(json)

                expectThat(result.expectSuccess().date).isEqualTo(LocalDateTime.of(2021, 2, 1, 2, 15, 56))
            }
        }

    }

    data class Transaction(val id: String, val dateTime: LocalDateTime)

    object JTransaction : JAny<Transaction>() {
        private val id by str(Transaction::id)
        private val date by str("dd/MM/yyyy hh:mm:ss a", Transaction::dateTime)

        override fun JsonNodeObject.deserializeOrThrow(): Transaction =
            Transaction(
                id = +id,
                dateTime = +date,
            )
    }

    data class TransactionWithOptionalDateTime(val id: String, val date: LocalDateTime?)

    object JTransactionWithOptionalDate : JAny<TransactionWithOptionalDateTime>() {
        private val id by str(TransactionWithOptionalDateTime::id)
        private val date by str("dd/MM/yyyy hh:mm:ss a", TransactionWithOptionalDateTime::date)

        override fun JsonNodeObject.deserializeOrThrow(): TransactionWithOptionalDateTime =
            TransactionWithOptionalDateTime(
                id = +id,
                date = +date,
            )
    }
}

