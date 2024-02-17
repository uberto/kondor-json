package com.ubertob.kondor.json

import com.ubertob.kondor.randomList
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class NdJsonTest {

    @Test
    fun `NdJson list of Person`() {

        repeat(10) {
            val ndJsonProducer = toNdJson(JPerson)

            val ndJsonReader = fromNdJson(JPerson)

            val personList = randomList(0, 100) { randomPerson() }.toSet()

            val lines = ndJsonProducer(personList)

            expectThat(lines.count()).isEqualTo(personList.size)

            val parsed = ndJsonReader(lines).expectSuccess()

            expectThat(parsed).containsExactlyInAnyOrder(personList)
        }
    }

    @Test
    fun `NdJson list of Person from stream`() {

        repeat(10) {

            val ndJsonProducer = toNdJsonStream(JPerson)

            val ndJsonReader = fromNdJsonStream(JPerson)

            val stream = ByteArrayOutputStream()

            val personList = randomList(0, 100) { randomPerson() }.toSet()

            ndJsonProducer(personList, stream)

            val jsonString = String(stream.toByteArray())


            expectThat(jsonString.count{it =='\n'}).isEqualTo(personList.size)

            val inputStream = ByteArrayInputStream(stream.toByteArray())
            val parsed = ndJsonReader(inputStream).expectSuccess()

            expectThat(parsed).containsExactlyInAnyOrder(personList)
        }
    }

}