package com.ubertob.kondor.json

import com.ubertob.kondor.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import kotlin.random.Random


val toothpaste = Product(1001, "paste", "toothpaste \"whiter than white\"", 12.34)
val offer = Product(10001, "special offer", "offer for custom fidality", null)

fun randomPerson() = Person(Random.nextInt(1, 1000), randomString(lowercase, 1, 10).capitalize())
fun randomCompany() = Company(randomString(lowercase, 5, 10), TaxType.values().random())

fun randomCustomer(): Customer = when (Random.nextBoolean()) {
    true -> randomPerson()
    false -> randomCompany()
}

fun randomProduct() = Product(
    Random.nextInt(1, 1000),
    randomString(text, 2, 10),
    randomText(100),
    randomNullable { randomPrice(10, 1000) })

fun randomInvoice() = Invoice(
    id = InvoiceId(randomString(digits, 5, 5)),
    vat = Random.nextBoolean(),
    customer = randomPerson(),
    items = randomList(1, 10) { randomProduct() },
    total = BigDecimal(randomPrice(10, 1000)),
    created = LocalDate.now(),
    paid = randomNullable { Instant.now().truncatedTo(SECONDS) }
)

fun randomCurrency() = Currency.getAvailableCurrencies().random()
fun randomMoney() = Money(randomCurrency(), randomPrice(100, 10000).toBigDecimal().toBigInteger())

fun randomExpenseReport() = ExpenseReport(
    randomPerson(),
    randomList(0, 10) { "expense_${it}" to randomMoney() }.toMap()
)

fun randomNotes() = Notes(
    updated = Instant.now().minusSeconds(Random.nextLong(100000)),
    thingsToDo = randomList(0, 10) { randomString(uppercase, 3, 3) to randomString(lowercase, 1, 20) }.toMap()
)

sealed class Customer()
data class Person(val id: Int, val name: String) : Customer()
data class Company(val name: String, val taxType: TaxType) : Customer()


object JPerson : JAny<Person>() {

    private val id by num(Person::id) // JField(Person::id, JInt)
    private val name by str(Person::name) //JField(Person::name, JString)

    override fun JsonNodeObject.deserializeOrThrow() =
        Person(
            id = +id,
            name = +name
        )
}


data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?)

object JProduct : JAny<Product>() {

    /* experimental

    private val id by binding(Product::id)
    private val long_description by binding(Product::longDesc )
    private val `short-desc` by binding(Product::shortDesc )
    private val price by binding(Product::price)

     */
    private val id by JField(Product::id, JInt)
    private val long_description by JField(Product::longDesc, JString)
    private val `short-desc` by JField(Product::shortDesc, JString)
    private val price by JFieldMaybe(Product::price, JDouble)

    override fun JsonNodeObject.deserializeOrThrow() =
        Product(
            id = +id,
            shortDesc = +`short-desc`,
            longDesc = +long_description,
            price = +price
        )
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
)

object JCompany : JAny<Company>() {

    private val name by JField(Company::name, JString)
    private val tax_type by JField(Company::taxType, JEnum(TaxType::valueOf))

    override fun JsonNodeObject.deserializeOrThrow(): Company? =
        Company(
            name = +name,
            taxType = +tax_type
        )
}

object JCustomer : JSealed<Customer> {

    override val discriminatorFieldName = "type"

    override val subConverters: Map<String, ObjectNodeConverter<out Customer>> =
        mapOf(
            "private" to JPerson,
            "company" to JCompany
        )

    override fun extractTypeName(obj: Customer): String =
        when (obj) {
            is Person -> "private"
            is Company -> "company"
        }

}


//JField(Invoice::id, JStringWrapper(::InvoiceId))
// JField(Invoice::vat, JBoolean)
// JField(Invoice::customer, JCustomer)
// JField(Invoice::items, JList(JProduct))
// JField(Invoice::total, JBigDecimal)
// JField(Invoice::created, JLocalDate)
// JFieldMaybe(Invoice::paid, JInstant)

object JInvoice : JAny<Invoice>() {

    val id by str(::InvoiceId, Invoice::id)
    val `vat-to-pay` by bool(Invoice::vat)
    val customer by obj(JCustomer, Invoice::customer)
    val items by array(JProduct, Invoice::items)
    val total by num(Invoice::total)
    val created_date by str(Invoice::created)
    val paid_datetime by num(Invoice::paid)

    override fun JsonNodeObject.deserializeOrThrow(): Invoice =
        Invoice(
            id = +id,
            vat = +`vat-to-pay`,
            customer = +customer,
            items = +items,
            total = +total,
            created = +created_date,
            paid = +paid_datetime
        )

}


data class Money(val currency: Currency, val amount: BigInteger)

object JMoney : JAny<Money>() {

    private val ccy by JField(Money::currency, JCurrency)
    private val amount by JField(Money::amount, JBigInteger)
    override fun JsonNodeObject.deserializeOrThrow() =
        Money(
            currency = +ccy,
            amount = +amount
        )
}

data class ExpenseReport(val person: Person, val expenses: Map<String, Money>)

object JExpenseReport : JAny<ExpenseReport>() {

    private val person by JField(ExpenseReport::person, JPerson)
    private val expenses by JField(ExpenseReport::expenses, JMap(JMoney))

    override fun JsonNodeObject.deserializeOrThrow() =
        ExpenseReport(
            person = +person,
            expenses = +expenses
        )
}

data class Notes(val updated: Instant, val thingsToDo: Map<String, String>)

object JNotes : JAny<Notes>() {
    private val updated by JField(Notes::updated, JInstant)
    private val things_to_do by JField(Notes::thingsToDo, JMap(JString))

    override fun JsonNodeObject.deserializeOrThrow() =
        Notes(
            updated = +updated,
            thingsToDo = +things_to_do
        )
}

class Products : ArrayList<Product>() {
    fun total(): Double = sumOf { it.price ?: 0.0 }

    companion object {
        fun fromIterable(from: Iterable<Product>): Products =
            from.fold(Products()) { acc, p -> acc.apply { add(p) } }
    }
}

object JProducts : JArray<Product, Products>() {
    override val converter = JProduct

    override fun convertToCollection(from: Iterable<Product>) =
        Products.fromIterable(from)

}


