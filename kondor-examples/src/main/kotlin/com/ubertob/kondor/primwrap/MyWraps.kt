package com.ubertob.kondor.primwrap

import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import java.time.Instant
import java.time.LocalDate

data class Age(override val raw: Int) : IntWrap(raw) {
    companion object : IntWrapCompanion(Age::class) {
        override fun fromPrimitive(primitive: Int) = Age(primitive)
    }
}

data class Name(override val raw: String) : StringWrap(raw) {
    companion object : StringWrapCompanion(Name::class) {
        override fun fromPrimitive(primitive: String) = Name(primitive)
    }
}

data class DoB(val doB: LocalDate) : StringWrap(doB.toString()) {
    companion object : StringWrapCompanion(DoB::class) {
        override fun fromPrimitive(primitive: String) = DoB(LocalDate.parse(primitive))
    }
}

data class RecordedAt(val instant: Instant) : LongWrap(instant.toEpochMilli()) {
    companion object : LongWrapCompanion(RecordedAt::class) {
        override fun fromPrimitive(primitive: Long) = RecordedAt(Instant.ofEpochMilli(primitive))
    }

}

data class Paid(val paid: Boolean) : BooleanWrap(paid)

data class UserW(val name: Name, val age: Age, val doB: DoB, val recordedAt: RecordedAt)


object JUserW : JAny<UserW>() {


    val name by strW(UserW::name)

    val age by numW(UserW::age)
    val doB by strW(UserW::doB)

    //    val paid by bool(UserW::paid)
    val recordedAt by numW(UserW::recordedAt)

    override fun JsonNodeObject.deserializeOrThrow() =
        UserW(
            name = +name,
            age = +age,
            doB = +doB,
            recordedAt = +recordedAt
        )

}


fun main() {
    //    val x = Age(23)
//    val y = Name("Fred")
    val fred1 = UserW(Name("Fred"), Age(23), DoB(LocalDate.of(2023,11, 17)), RecordedAt(Instant.ofEpochMilli(1700212086459)))

    val fred = JUserW.fromJson(
        """{"name": "Fred", "age": 23, "doB": "2023-11-17", "recordedAt": 1700212086459}"""
    ).orThrow()

    assert(fred1 == fred)

    println(intWrappersMap)


    val age: Age = createTinyTypeInt(42)

    val name: Name = createTinyTypeString("John")

    println("$name age is $age")

    val json = JUserW.toJson(fred)

    println(json)

    val f2 = JUserW.fromJson(json).orThrow()

    println(f2 == fred)


}