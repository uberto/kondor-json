package com.ubertob.kondor.json

import com.ubertob.kondor.json.datetime.JLocalTime
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class JLocalTimeTest {

    @Test
    fun `Json LocalTime with custom format`() {
        val time = LocalTime.of(14, 1, 34)
        val format = DateTimeFormatter.ofPattern("hh:mm:ss a").withLocale(Locale.ENGLISH)
        val jLocalTime = JLocalTime.withFormatter(format)

        val timeAsJsonNode = JsonNodeString("02:01:34 PM")
        expectThat(jLocalTime.toJsonNode(time)).isEqualTo(timeAsJsonNode)
        expectThat(jLocalTime.fromJsonNode(timeAsJsonNode, NodePathRoot).expectSuccess()).isEqualTo(time)

        val timeAsString = "\"02:01:34 PM\""
        expectThat(jLocalTime.toJson(time)).isEqualTo(timeAsString)
        expectThat(jLocalTime.fromJson(timeAsString).expectSuccess()).isEqualTo(time)
    }

    @Test
    fun `Json LocalTime with custom format as String`() {
        val time = LocalTime.of(14, 1, 34)
        val jLocalTime = JLocalTime.withPattern("hh:mm:ss a")

        val timeAsJsonNode = JsonNodeString("02:01:34 PM")
        expectThat(jLocalTime.toJsonNode(time)).isEqualTo(timeAsJsonNode)
        expectThat(jLocalTime.fromJsonNode(timeAsJsonNode, NodePathRoot).expectSuccess()).isEqualTo(time)

        val timeAsString = "\"02:01:34 PM\""
        expectThat(jLocalTime.toJson(time)).isEqualTo(timeAsString)
        expectThat(jLocalTime.fromJson(timeAsString).expectSuccess()).isEqualTo(time)
    }

    @Nested
    inner class ShortFunctions {
        @Test
        fun `reads an object with time in a custom format`() {
            val json =
                // language=json
                """
                    {
                      "id": "abcd",
                      "time": "02:01:34 PM"
                    }
                """.trimIndent()

            val result = JTransaction.fromJson(json)

            expectThat(result.expectSuccess().time).isEqualTo(LocalTime.of(14, 1, 34))
        }

        @Nested
        @DisplayName("reads an object with optional time")
        inner class OptionalTime {
            @Test
            fun `time missing`() {
                val json =
                    // language=json
                    """
                        {
                          "id": "abcd"
                        }
                    """.trimIndent()

                val result = JTransactionWithOptionalTime.fromJson(json)

                expectThat(result.expectSuccess().time).isEqualTo(null)
            }

            @Test
            fun `time present`() {
                val json =
                    // language=json
                    """
                    {
                      "id": "abcd",
                      "time": "02:15:56 AM"
                    }
                """.trimIndent()

                val result = JTransactionWithOptionalTime.fromJson(json)

                expectThat(result.expectSuccess().time).isEqualTo(LocalTime.of(2, 15, 56))
            }
        }

    }

    data class Transaction(val id: String, val time: LocalTime)

    object JTransaction : JAny<Transaction>() {
        private val id by str(Transaction::id)
        private val time by str("hh:mm:ss a", Transaction::time)

        override fun JsonNodeObject.deserializeOrThrow(): Transaction =
            Transaction(
                id = +id,
                time = +time,
            )
    }

    data class TransactionWithOptionalTime(val id: String, val time: LocalTime?)

    object JTransactionWithOptionalTime : JAny<TransactionWithOptionalTime>() {
        private val id by str(TransactionWithOptionalTime::id)
        private val time by str("hh:mm:ss a", TransactionWithOptionalTime::time)

        override fun JsonNodeObject.deserializeOrThrow(): TransactionWithOptionalTime =
            TransactionWithOptionalTime(
                id = +id,
                time = +time,
            )
    }
}
