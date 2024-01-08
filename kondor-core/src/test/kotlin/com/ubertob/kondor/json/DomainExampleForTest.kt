package com.ubertob.kondor.json

import com.ubertob.kondor.*
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.TitleType.Companion.fromLabel
import com.ubertob.kondor.json.datetime.num
import com.ubertob.kondor.json.datetime.str
import com.ubertob.kondor.json.jsonnode.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import kotlin.random.Random

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

fun randomTaskId(): TaskId = UUID.randomUUID().toString().let(::TaskId)
fun randomTask(): Task = Task(randomString(uppercase, 3, 10), randomString(lowercase, 1, 20))
fun randomTasks(): Map<TaskId, Task> = randomList(0, 10) { randomTaskId() to randomTask() }.toMap()

private fun randomPath() =
    randomList(1, 10, { randomString(lowercase, 3, 10) }).joinToString(separator = "/", prefix = "/")

private fun randomInstant() = Instant.ofEpochMilli(Random.nextLong())

fun randomFileInfo() = FileInfo(
    name = randomString(lowercase, 1, 20),
    date = randomInstant(),
    isDir = Random.nextBoolean(),
    size = Random.nextLong(100000),
    folderPath = randomPath()
)

fun randomMetadata(): Map<String, String> =
    randomList(0, 10) {
        randomString(lowercase, 4, 6) to randomString(text, 1, 50)
    }.toMap()


fun randomObjectWithDynamicAttr(): DynamicAttr = DynamicAttr(
    id = Random.nextInt(1, 1000),
    name = randomString(lowercase, 1, 10),
    attributes = JsonNodeObject(
        randomNodeFields(), NodePathRoot
    )
)

fun randomNodeFields(): FieldMap =
    mapOf(
        "bool_f" to JsonNodeBoolean(Random.nextBoolean(), NodePathRoot + "bool_f"),
        "double_f" to JsonNodeNumber(Random.nextDouble().toBigDecimal(), NodePathRoot + "double_f"),
        "string_f" to JsonNodeString(randomString(uppercase, 1, 10), NodePathRoot + "string_f")
    )

//------------

sealed class Customer()
data class Person(val id: Int, val name: String) : Customer()
data class Company(val name: String, val taxType: TaxType) : Customer()
object AnonymousCustomer : Customer()

data class GraphNode(val name: String, val nodeType: String, val path: String)

//converters

object JStringList : JArrayConverter<List<String>> by JList(JString)

object JGraphNode : JAny<GraphNode>() {

    private val name by str(GraphNode::name)
    private val nodeType by str(GraphNode::nodeType)
    private val path by str(GraphNode::path)

    override fun JsonNodeObject.deserializeOrThrow() =
        GraphNode(
            name = +name,
            nodeType = +nodeType,
            path = +path
        )
}


object JPerson : JAny<Person>() {

    private val id by num(Person::id)
    private val name by str(Person::name)

    override fun JsonNodeObject.deserializeOrThrow() =
        Person(
            id = +id,
            name = +name
        )
}


data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?)

object JProduct : JAny<Product>() {

    private val id by num(Product::id)
    private val long_description by str(Product::longDesc)
    private val `short-desc` by str(Product::shortDesc)
    private val price by num(Product::price)

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

    private val name by str(Company::name)
    private val tax_type by str(Company::taxType)

    override fun JsonNodeObject.deserializeOrThrow() =
        Company(
            name = +name,
            taxType = +tax_type
        )
}

object JCustomer : JSealed<Customer>() {

    override val discriminatorFieldName = "type"

    override val subConverters: Map<String, ObjectNodeConverterWriters<out Customer>> =
        mapOf(
            "private" to JPerson,
            "company" to JCompany,
            "anonymous" to JInstance(AnonymousCustomer)
        )

    override fun extractTypeName(obj: Customer): String =
        when (obj) {
            is Person -> "private"
            is Company -> "company"
            AnonymousCustomer -> "anonymous"
        }

}


object JInvoice : JAny<Invoice>() {

    private val id by str(::InvoiceId, Invoice::id)
    private val `vat-to-pay` by bool(Invoice::vat)
    private val customer by obj(JCustomer, Invoice::customer)
    private val items by array(JProduct, Invoice::items)
    private val total by num(Invoice::total)
    private val created_date by str(Invoice::created)
    private val paid_datetime by num(Invoice::paid)

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

    private val ccy by str(Money::currency)
    private val amount by num(Money::amount)

    override fun JsonNodeObject.deserializeOrThrow() =
        Money(
            currency = +ccy,
            amount = +amount
        )
}

data class ExpenseReport(val person: Person, val expenses: Map<String, Money>)

object JExpenseReport : JAny<ExpenseReport>() {

    private val person by obj(JPerson, ExpenseReport::person)
    private val expenses by obj(JMap(JMoney), ExpenseReport::expenses)

    override fun JsonNodeObject.deserializeOrThrow() =
        ExpenseReport(
            person = +person,
            expenses = +expenses
        )
}

data class Notes(val updated: Instant, val thingsToDo: Map<String, String>)

object JNotes : JAny<Notes>() {
    private val updated by str(Notes::updated)
    private val things_to_do by obj(JMap(JString), Notes::thingsToDo)

    override fun JsonNodeObject.deserializeOrThrow() =
        Notes(
            updated = +updated,
            thingsToDo = +things_to_do
        )
}

data class TaskId(val value: String)
object JTaskId : JStringRepresentable<TaskId>() {
    override val cons: (String) -> TaskId = ::TaskId
    override val render: (TaskId) -> String = TaskId::value

}

data class Task(val name: String, val description: String)
object JTask : JAny<Task>() {
    private val name by str(Task::name)
    private val description by str(Task::description)

    override fun JsonNodeObject.deserializeOrThrow() =
        Task(
            name = +name,
            description = +description
        )
}

val JTasks: JMap<TaskId, Task> = JMap(JTaskId, JTask)


class Products : ArrayList<Product>() {
    fun total(): Double = sumOf { it.price ?: 0.0 }

    companion object {
        fun fromIterable(from: Iterable<Product>): Products =
            from.fold(Products()) { acc, p -> acc.apply { add(p) } }
    }
}

//custom collection
object JProducts : JArray<Product, Products> {
    override val converter = JProduct

    override fun convertToCollection(from: Iterable<Product>) =
        Products.fromIterable(from)

    override val _nodeType = ArrayNode

}

data class FileInfo(val name: String, val date: Instant, val isDir: Boolean, val size: Long, val folderPath: String)

object JFileInfo : JAny<FileInfo>() {
    val file_name by str(FileInfo::name)
    val creation_date by num(FileInfo::date)
    val is_dir by bool(FileInfo::isDir)
    val size by num(FileInfo::size)
    val folder_path by str(FileInfo::folderPath)

    override fun JsonNodeObject.deserializeOrThrow() =
        FileInfo(
            name = +file_name,
            date = +creation_date,
            isDir = +is_dir,
            size = +size,
            folderPath = +folder_path
        )
}

data class MetadataFile(val filename: String, val metadata: Map<String, String>)

object JMetadataFile : JAny<MetadataFile>() {

    val fileName by str(MetadataFile::filename)
    val metadata by flatten(JMap(), MetadataFile::metadata)

    override fun JsonNodeObject.deserializeOrThrow() =
        MetadataFile(
            filename = +fileName,
            metadata = +metadata
        )
}

data class SelectedFile(val selected: Boolean, val file: FileInfo)

object JSelectedFile : JAny<SelectedFile>() {

    val selected by bool(SelectedFile::selected)
    val file_info by flatten(JFileInfo, SelectedFile::file)

    override fun JsonNodeObject.deserializeOrThrow() =
        SelectedFile(
            selected = +selected,
            file = +file_info,
        )

}


// Using other field of Enum
data class TitleRequest(
    val id: String,
    val type: TitleType?
)

enum class TitleType(val label: String) {
    Movie("movie"), Series("series"), Episode("episode");

    companion object {
        fun fromLabel(label: String) = values().firstOrNull() { it.label == label }
    }
}

object JTitleType : JStringRepresentable<TitleType?>() {
    override val cons: (String) -> TitleType? = ::fromLabel
    override val render: (TitleType?) -> String = { it?.label.orEmpty() }
}

object JTitleRequest : JAny<TitleRequest>() {
    private val id by str(TitleRequest::id)

    private val type by str(JTitleType, TitleRequest::type)

    override fun JsonNodeObject.deserializeOrThrow(): TitleRequest =
        TitleRequest(
            id = +id,
            type = +type
        )
}

// sealed with default

sealed class Variant {
    abstract val name: String
}

data class VariantString(override val name: String, val value: String) : Variant()

data class VariantInt(override val name: String, val value: Int) : Variant()

fun randomVariant() = when (Random.nextBoolean()) {
    true -> VariantString(randomString(lowercase, 5, 5), randomText(20))
    false -> VariantInt(randomString(lowercase, 5, 5), Random.nextInt())
}

object JVariantString : JAny<VariantString>() {

    private val name by str(VariantString::name)

    private val value by str(VariantString::value)

    override fun JsonNodeObject.deserializeOrThrow() =
        VariantString(
            +name,
            +value
        )

}

object JVariantInt : JAny<VariantInt>() {

    private val name by str(VariantInt::name)

    private val value by num(VariantInt::value)

    override fun JsonNodeObject.deserializeOrThrow() =
        VariantInt(
            +name,
            +value
        )

}

object JVariant : JSealed<Variant>() {

    private val STR = "VariantString"
    private val INT = "VariantInt"

    override val discriminatorFieldName = "type"
    override val defaultConverter = JVariantString

    override fun extractTypeName(obj: Variant) =
        when (obj) {
            is VariantInt -> INT
            is VariantString -> STR
        }

    override val subConverters =
        mapOf(
            STR to JVariantString,
            INT to JVariantInt
        )

}

data class DynamicAttr(
    val id: Int,
    val name: String,
    val attributes: JsonNodeObject
)

object JDynamicAttr : JAny<DynamicAttr>() {
    private val id by num(DynamicAttr::id)
    private val name by str(DynamicAttr::name)
    private val attributes by flatten(DynamicAttr::attributes)
    override fun JsonNodeObject.deserializeOrThrow() = DynamicAttr(
        id = +id,
        name = +name,
        attributes = +attributes
    )
}


data class OptionalAddress(val name: String, val street: String?, val city: String?)

object JOptionalAddressPretty : JAny<OptionalAddress>() {

    override val jsonStyle = prettyWithNulls

    val name by str(OptionalAddress::name)
    val street by str(OptionalAddress::street)
    val city by str(OptionalAddress::city)

    override fun JsonNodeObject.deserializeOrThrow() = OptionalAddress(
        +name,
        +street,
        +city
    )
}