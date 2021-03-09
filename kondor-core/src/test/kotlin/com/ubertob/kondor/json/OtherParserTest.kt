package com.ubertob.kondor.json

import JsonLexer
import com.ubertob.kondor.expectSuccess
import org.junit.jupiter.api.Test

class OtherParserTest {

    @Test
    fun `parse liquibase Json format`() {
        //example from  liquibase.com
        val json =
            """{"databaseChangeLog":[{"preConditions":[{"runningAs":{"username":"liquibase"}}]},{"changeSet":{"id":"1","author":"nvoxland","changes":[{"createTable":{"tableName":"person","columns":[{"column":{"name":"id","type":"int","autoIncrement":true,"constraints":{"primaryKey":true,"nullable":false}}},{"column":{"name":"firstname","type":"varchar(50)"}},{"column":{"name":"lastname","type":"varchar(50)","constraints":{"nullable":false}}},{"column":{"name":"state","type":"char(2)"}}]}}]}},{"changeSet":{"id":"2","author":"nvoxland","changes":[{"addColumn":{"tableName":"person","columns":[{"column":{"name":"username","type":"varchar(8)"}}]}}]}},{"changeSet":{"id":"3","author":"nvoxland","changes":[{"addLookupTable":{"existingTableName":"person","existingColumnName":"state","newTableName":"state","newColumnName":"id","newColumnDataType":"char(2)"}}]}}]}"""


        val node = parseJsonNodeObject(JsonLexer.tokenize(json), NodeRoot).expectSuccess()

        println(node.fieldMap)


        val dcl = JDatabaseChangeLog.fromJson(json).expectSuccess()

        println(dcl)
    }

    interface ChangeLogItem

    data class Column(val name: String, val type: String)
    sealed class Change {
        data class CreateTable(val tableName: String, val columns: List<Column>) : Change()
        data class AddColumn(val tableName: String, val columns: List<Column>) : Change()
        data class AddLookupTable(
            val existingTableName: String,
            val existingColumnName: String,
            val newTableName: String,
            val newColumnName: String,
            val newColumnDataType: String
        ) : Change()
    }

    data class Preconditions(val preconditions: List<Precondition>) : ChangeLogItem
    data class Precondition(val runningAs: String?, val dbms: String?)
    data class ChangeSet(val id: String, val author: String, val changes: List<Change>) : ChangeLogItem
    data class DatabaseChangeLog(val preconditions: Preconditions, val changeSets: List<ChangeSet>)

    object JPreconditions : JAny<Preconditions>() {
        override fun JsonNodeObject.deserializeOrThrow(): Preconditions? =
            Preconditions(emptyList())

    }

    object JAddColumn : JAny<Change.AddColumn>() {

        //        val tableName by
        override fun JsonNodeObject.deserializeOrThrow() =
            TODO() //  Change.AddColumn()

    }

    object JAddLookupTable : JAny<Change.AddLookupTable>() {

        //        val tableName by
        override fun JsonNodeObject.deserializeOrThrow() =
            TODO() //  Change.AddColumn()

    }

    object JCreateTable : JAny<Change.CreateTable>() {

        //        val tableName by
        override fun JsonNodeObject.deserializeOrThrow() =
            TODO() //  Change.AddColumn()

    }

    object JChange : NestedPolyConverter<Change> {

        override fun extractTypeName(obj: Change): String =
            when (obj) {
                is Change.AddColumn -> "addColumn"
                is Change.AddLookupTable -> "addLookupTable"
                is Change.CreateTable -> "createTable"
            }

        override val subConverters = mapOf(
            "addColumn" to JAddColumn,
            "addLookupTable" to JAddLookupTable,
            "createTable" to JCreateTable
        )
    }

    object JChangeSet : JAny<ChangeSet>() {
        val id by JField(ChangeSet::id, JString)
        val author by JField(ChangeSet::author, JString)
        val changes by JField(ChangeSet::changes, JList(JChange))

        override fun JsonNodeObject.deserializeOrThrow() =
            ChangeSet("id", "author", emptyList())
    }


    object JChangeLogItem : NestedPolyConverter<ChangeLogItem> {
        override fun extractTypeName(obj: ChangeLogItem): String =
            when (obj) {
                is Preconditions -> "preConditions"
                is ChangeSet -> "changeSet"
                else -> error("!!! $obj not expected")
            }

        override val subConverters: Map<String, ObjectNodeConverter<out ChangeLogItem>> =
            mapOf("preConditions" to JPreconditions, "changeSet" to JChangeSet)


    }

    object JDatabaseChangeLog : JAny<DatabaseChangeLog>() {
        val databaseChangeLog by JField({ it.changeSets + it.preconditions }, JList(JChangeLogItem))

        override fun JsonNodeObject.deserializeOrThrow() =
            DatabaseChangeLog(
                preconditions = (+databaseChangeLog).filterIsInstance<Preconditions>().firstOrNull() ?: Preconditions(emptyList()),
                changeSets = (+databaseChangeLog).filterIsInstance<ChangeSet>()
            )
    }
}