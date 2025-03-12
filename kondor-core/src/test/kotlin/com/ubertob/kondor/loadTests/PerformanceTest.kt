package com.ubertob.kondor.loadTests

import com.ubertob.kondor.all
import com.ubertob.kondor.chronoAndLog
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.onRoot
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.randomString
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.File

/*
On my laptop: 4/5/2023

JInvoices 50k Invoices, 63MB
serialization 1300 ms
serialization compact 1157 ms
total parsing 1339 ms
tokenizing 672 ms
toJsonNode 524 ms
marshalling 224 ms


JFileInfo 100k 15MB
serialization 282 ms
serialization compact 220 ms
total parsing 470 ms
tokenizing 163 ms
toJsonNode 303 ms
marshalling 70 ms

JStrings 100k 1.6Mb
serialization 11 ms
serialization compact 10 ms
total parsing 20 ms
tokenizing 31 ms
toJsonNode 22 ms
marshalling 3 ms

On my laptop: 21/9/2023
JInvoices 50k Invoices, 63MB
serialization 1165 ms
serialization compact 1319 ms
total parsing 15143 ms
total lazy parsing 2140 ms
tokenizing 3515 ms
parsing up to JsonNode 11839 ms
marshalling 2771 ms

FileInfo Json String length 15M
serialization 207 ms
serialization compact 213 ms
total parsing 1629 ms
total lazy parsing 577 ms
tokenizing 93 ms
parsing up to JsonNode 270 ms
marshalling 38 ms
lazy parsing 769 ms

Strings Json String length 1.6M
serialization 8 ms
serialization compact 8 ms
total parsing 19 ms
tokenizing 8 ms
parsing up to JsonNode 8 ms
marshalling 3 ms
lazy parsing 46 ms

On my laptop: 17/10/2023
JFileInfo 100k 15MB
serialization 174 ms
serialization compact 168 ms
total parsing 417 ms
tokenizing 170 ms
parsing up to JsonNode 237 ms
marshalling 102 ms
lazy parsing 625 ms

JInvoices 50k Invoices, 63MB
tokenizing 906 ms
parsing up to JsonNode 13796 ms
marshalling 2788 ms
serialization 1015 ms
serialization compact 1192 ms
total parsing 16595 ms

Strings Json String length 1.6M
serialization 672 ms
serialization compact 635 ms
total parsing 377 ms
tokenizing 367 ms
parsing up to JsonNode 13 ms
marshalling 3 ms
lazy parsing 2947 ms

On my laptop: 28/01/2025

JFileInfo
serialization 89 ms
serialization compact 74 ms
total parsing 278 ms
tokenizing 116 ms
parsing up to JsonNode 208 ms
marshalling 44 ms
lazy parsing 885 ms

JInvoice 50k
serialization 424 ms
serialization compact 433 ms
total parsing 1292 ms
tokenizing 527 ms
parsing up to JsonNode 606 ms
marshalling 198 ms

Array of strings
serialization 741 ms
serialization compact 754 ms
total parsing 742 ms
tokenizing 722 ms
parsing up to JsonNode 14 ms
marshalling 3 ms
lazy parsing 4569 ms

50k Invoices parsing from stream 3446 ms
 */

@Disabled
class PerformanceTest {

    val times = 10

    @Test
    fun `serialize and parse invoices`() {

        val JInvoices = JList(JInvoice)

        val invoices = generateSequence(0) { it + 1 }.take(50_000).map {
            randomInvoice().copy(id = InvoiceId(it.toString()))
        }.toList()

        println("Invoices Json String length ${JInvoices.toJson(invoices).length}")
        repeat(times) {

            val jsonString = chronoAndLog("serialization") { JInvoices.toJson(invoices) }

            chronoAndLog("serialization compact") { JInvoices.toJson(invoices, JsonStyle.compact) }

            chronoAndLog("total parsing") { JInvoices.fromJson(jsonString) }.expectSuccess()

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString) }.expectSuccess()

            val nodes = chronoAndLog("parsing up to JsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

//!!! broken atm            chronoAndLog("marshalling") { JInvoices.fromJsonNode(nodes, NodePathRoot) }

//            chronoAndLog("lazy parsing") {
//                ByteArrayInputStream(jsonString.toByteArray()).use {
//                    JInvoices.fromJson(it).expectSuccess()
//                }
//            }

        }

    }


    @Test
    fun `serialize and parse FileInfo`() {

        val jFileInfos = JList(JFileInfo)
        val jFileInfosNew = JList(JFileInfoNew)

        val fileInfos = generateSequence(0) { it + 1 }.take(100_000).map {
            randomFileInfo().copy(name = it.toString())
        }.toList()

        println("FileInfo Json String length ${jFileInfos.toJson(fileInfos).length}")
        repeat(times) {

            val jsonString = chronoAndLog("serialization") { jFileInfos.toJson(fileInfos) }

            chronoAndLog("serialization compact") { jFileInfos.toJson(fileInfos, JsonStyle.compact) }

            val olds = chronoAndLog("total parsing") { jFileInfos.fromJson(jsonString) }
            val news = chronoAndLog("total parsing NEW") { jFileInfosNew.fromJson(jsonString) }

            expectThat(olds).isEqualTo(news)

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString).expectSuccess() }

            val nodes = chronoAndLog("parsing up to JsonNode") { ArrayNode.parse(tokens.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jFileInfos.fromJsonNode(nodes, NodePathRoot) }

            chronoAndLog("lazy parsing") {
                jFileInfos.fromJson(ByteArrayInputStream(jsonString.toByteArray())).expectSuccess()
            }

        }

    }

    @Test
    fun `serialize and parse array of strings`() {

        val jStrings = JList(JString)

        val strings = generateSequence(0) { it + 1 }.take(100_000).map {
            "string $it " + randomString(all, 1000, 1000)
        }.toList()

        println("Strings Json String length ${jStrings.toJson(strings).length}")
        repeat(times) {

            val jsonString = chronoAndLog("serialization") { jStrings.toJson(strings) }

            chronoAndLog("serialization compact") { jStrings.toJson(strings, JsonStyle.compact) }

            chronoAndLog("total parsing") { jStrings.fromJson(jsonString) }

            val tokens = chronoAndLog("tokenizing") { KondorTokenizer.tokenize(jsonString).expectSuccess() }

            val values = chronoAndLog("parsing from tokens to value") {
                jStrings.fromTokens(tokens, NodePathRoot).expectSuccess()
            }

            val tokens2 = KondorTokenizer.tokenize(jsonString).expectSuccess()

            val nodes =
                chronoAndLog("parsing from tokens to JsonNode") { ArrayNode.parse(tokens2.onRoot()) }.expectSuccess()

            chronoAndLog("marshalling") { jStrings.fromJsonNode(nodes, NodePathRoot) }

            val jsonStream = ByteArrayInputStream(jsonString.toByteArray())
            chronoAndLog("lazy parsing") {
                jStrings.fromJson(jsonStream).expectSuccess()
            }

        }

    }


    @Test
    fun `using inputstream to parse invoices`() {

        val JInvoices = JList(JInvoice)

        val fixtureName = "/fixtures/invoices.json.ignoreme" //delete this file if the format change
        val fixtureFile = File("./src/test/resources/$fixtureName")
        val inputStreamTotInvoices = 50_000

        if (!fixtureFile.exists()) {
            println("creating ${fixtureFile.absolutePath}")
            val invoices = generateSequence(0) { it + 1 }.take(inputStreamTotInvoices).map {
                randomInvoice().copy(id = InvoiceId(it.toString()))
            }.toList()

            fixtureFile.writeText(JInvoices.toJson(invoices))
        }

        val inputStream = javaClass.getResourceAsStream(fixtureName) ?: error("resource $fixtureName not found!")

        chronoAndLog("parsing from stream") {
            val invoices = JInvoices.fromJson(inputStream).expectSuccess()
            expectThat(invoices.size).isEqualTo(inputStreamTotInvoices)
        }

    }
}



