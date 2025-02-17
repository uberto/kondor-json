package com.ubertob.kondor.json

import com.ubertob.kondor.*
import com.ubertob.kondor.json.datetime.num
import com.ubertob.kondor.json.datetime.str
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.random.Random

sealed class Customer() {
    object Json : JSealed<Customer>() {

        override val discriminatorFieldName = "type"

        override val subConverters: Map<String, ObjectNodeConverterWriters<out Customer>> =
            mapOf(
                "private" to Person.Json,
                "company" to Company.Json,
                "anonymous" to JInstance(AnonymousCustomer)
            )

        override fun extractTypeName(obj: Customer): String =
            when (obj) {
                is Person -> "private"
                is Company -> "company"
                AnonymousCustomer -> "anonymous"
            }

    }

}

data class Person(val id: Int, val name: String) : Customer() {
    object Json : JDataClass<Person>(Person::class) {
        val id by num(Person::id)
        val name by str(Person::name)
    }
}

data class Company(val name: String, val taxType: TaxType) : Customer() {
    object Json : JDataClass<Company>(Company::class) {
        val name by str(Company::name)
        val taxType by str(Company::taxType)
    }
}

object AnonymousCustomer : Customer()


data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?) {
    object Json : JDataClass<Product>(Product::class) {
        val id by num(Product::id)
        val `short-desc` by str(Product::shortDesc)
        val long_description by str(Product::longDesc)
        val price by num(Product::price)
    }
}


data class InvoiceId(override val raw: String) : StringWrapper

enum class TaxType {
    Domestic, Exempt, EU, US, Other
}


data class Invoice(
    val id: InvoiceId,
    val vat: Boolean,
    val customer: Customer,
    val items: List<Product>,
    val total: BigDecimal,
    val created: LocalDate,
    val paid: Instant?
) {
    object Json : JDataClass<Invoice>(Invoice::class) {
        val id by str(::InvoiceId, Invoice::id)
        val `vat-to-pay` by bool(Invoice::vat)
        val customer by obj(Customer.Json, Invoice::customer)
        val items by array(Product.Json, Invoice::items)
        val total by num(Invoice::total)
        val created_date by str(Invoice::created)
        val paid_datetime by num(Invoice::paid)
    }
}


fun randomPerson() = Person(Random.nextInt(1, 1000), randomString(lowercase, 1, 10).replaceFirstChar { it.uppercase() })
fun randomCompany() = Company(randomString(lowercase, 5, 10), TaxType.values().random())

fun randomCustomer(): Customer = when (Random.nextBoolean()) {
    true -> randomPerson()
    false -> randomCompany()
}

fun randomProduct() = Product(
    id = Random.nextInt(1, 1000),
    shortDesc = randomString(lowercase, 2, 10),
    longDesc = randomText(100),
    price = randomNullable { randomPrice(10, 1000) })

fun randomInvoice() = Invoice(
    id = InvoiceId(randomString(digits, 5, 5)),
    vat = Random.nextBoolean(),
    customer = randomCustomer(),
    items = randomList(1, 10) { randomProduct() },
    total = BigDecimal(randomPrice(10, 1000)),
    created = LocalDate.now(),
    paid = randomNullable { Instant.now().truncatedTo(SECONDS) }
)
