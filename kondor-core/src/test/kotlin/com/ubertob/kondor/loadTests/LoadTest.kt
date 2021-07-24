package com.ubertob.kondor.loadTests

import com.ubertob.kondor.json.InvoiceId
import com.ubertob.kondor.json.JInvoice
import com.ubertob.kondor.json.JList
import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.onRoot
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.randomInvoice
import com.ubertob.kondor.outcome.bind
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


// 2021 07 22 - 30k Invoices, 37MB, ser 1400ms. parsing 5300ms.  [tokenizing 2800 ms toJsonNode 1800 ms marshalling 700 ms]
/*
24 July
serialization 1575 ms
total parsing 3326 ms
tokenizing 648 ms
toJsonNode 2321 ms
marshalling 691 ms
 */


class LoadTest {


        @Disabled
    @Test
    fun `serialize and parse invoices`() {

        val JInvoices = JList(JInvoice)

        val invoices = generateSequence(0) { it + 1 }.take(30_000).map {
            randomInvoice().copy(id = InvoiceId(it.toString()))
        }.toList()

        println("Json String length ${JInvoices.toJson(invoices).length}")
        repeat(100) {


            System.gc()

            val jsonString = chronoAndLog("serialization") { JInvoices.toJson(invoices) }

            chronoAndLog("total parsing") { JInvoices.fromJson(jsonString) }

            chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) } //add for each for lazy

            chronoAndLog("toJsonNode") { KondorTokenizer.tokenize(jsonString).run { ArrayNode.parse(onRoot()) } }.bind {
               chronoAndLog("marshalling") { JInvoices.fromJsonNode(it) }
            }


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

