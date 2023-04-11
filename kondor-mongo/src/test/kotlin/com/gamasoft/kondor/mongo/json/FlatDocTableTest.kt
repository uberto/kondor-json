package com.gamasoft.kondor.mongo.json

import com.gamasoft.kondor.mongo.core.MongoConnectionTest.Companion.dbName
import com.gamasoft.kondor.mongo.core.MongoConnectionTest.Companion.mongoConnection
import com.gamasoft.kondor.mongo.core.MongoExecutor
import com.gamasoft.kondor.mongo.core.TypedTable
import com.gamasoft.kondor.mongo.core.mongoOperation
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate

class FlatDocTableTest {

    private object FlatDocs: TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
        override val collectionName: String = "FlatDocs"
        //retention... policy.. index
    }



    private fun createDoc(i: Int): SimpleFlatDoc =
        SimpleFlatDoc(
            index = i,
            name = "mydoc $i",
            date = LocalDate.now().minusDays(i.toLong()),
            bool = i % 2 == 0
        )

    private val doc = createDoc(0)

    val oneDocReader = mongoOperation {
        FlatDocs.drop()
        FlatDocs.addDocument(doc)
        val docs = FlatDocs.all()
        expectThat(1).isEqualTo( docs.count())
        docs.first()
    }

    val docQueryReader = mongoOperation {
        (1..100).forEach {
            FlatDocs.addDocument(createDoc(it))
        }
        FlatDocs.find("{ index: 42 }").first()
    }


    @Test
    fun `add and query doc safely`() {
        val provider = MongoExecutor(mongoConnection, dbName)

        val myDoc = provider(oneDocReader).expectSuccess()
        expectThat(doc).isEqualTo( myDoc)

    }


    @Test
    fun `parsing query safely`() {
        val provider = MongoExecutor(mongoConnection, dbName)

        val myDoc = provider(docQueryReader).expectSuccess()
        expectThat(42).isEqualTo( myDoc.index)

    }
}


