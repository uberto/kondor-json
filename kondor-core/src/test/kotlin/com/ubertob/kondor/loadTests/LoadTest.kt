package com.ubertob.kondor.loadTests

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.onRoot
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/*
JInvoices 50k Invoices, 63MB
serialization 2175 ms
total parsing 2501 ms
tokenizing 1146 ms
toJsonNode 757 ms
marshalling 297 ms


JFileInfo 100k 15MB

serialization 318 ms
total parsing 592 ms
tokenizing 248 ms
toJsonNode 233 ms
marshalling 101 ms

JStrings 100k 1.6Mb
serialization 14 ms
total parsing 30 ms
tokenizing 16 ms
toJsonNode 11 ms
marshalling 3 ms

 */

@Disabled
class LoadTest {


    @Test
    fun `serialize and parse invoices`() {

        val JInvoices = JList(JInvoice)

        val invoices = generateSequence(0) { it + 1 }.take(50_000).map {
            randomInvoice().copy(id = InvoiceId(it.toString()))
        }.toList()

        println("Json String length ${JInvoices.toJson(invoices).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { JInvoices.toJson(invoices) }

            chronoAndLog("total parsing") { JInvoices.fromJson(jsonString) }

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for each for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { JInvoices.fromJsonNode(nodes) }

        }

    }



    @Test
    fun `serialize and parse FileInfo`() {

        val jFileInfos = JList(JFileInfo)

        val invoices = generateSequence(0) { it + 1 }.take(100_000).map {
            randomFileInfo().copy(name = it.toString())
        }.toList()

        println("Json String length ${jFileInfos.toJson(invoices).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { jFileInfos.toJson(invoices) }

            chronoAndLog("total parsing") { jFileInfos.fromJson(jsonString) }

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for eaJFileInfosch for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jFileInfos.fromJsonNode(nodes) }

        }

    }

    @Test
    fun `serialize and parse array of strings`() {

        val jStrings = JList(JString)

        val strings = generateSequence(0) { it + 1 }.take(100_000).map {
            "string $it"
        }.toList()

        println("Json String length ${jStrings.toJson(strings).length}")
        repeat(100) {

            val jsonString = chronoAndLog("serialization") { jStrings.toJson(strings) }

            chronoAndLog("total parsing") { jStrings.fromJson(jsonString) }

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for eaJFileInfosch for lazy

            val nodes = chronoAndLog("toJsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jStrings.fromJsonNode(nodes) }

        }

    }

}

fun <T> chronoAndLog(logPrefix: String, fn: () -> T): T {
    val start = System.nanoTime()
    val res = fn()
    val elapsed = System.nanoTime() - start

    println("$logPrefix ${elapsed / 1_000_000} ms")
    return res
}

