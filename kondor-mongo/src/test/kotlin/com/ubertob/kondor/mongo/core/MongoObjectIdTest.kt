import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Testcontainers
class MongoObjectIdTest {


    private val localMongo = MongoExecutorDbClient.fromConnectionString(
        connection = MongoTableTest.mongoConnection,
        databaseName = "mongoCollTest"
    )

    object keyValueStoreTable : TypedTable<KeyValueStore>(JKeyValueStore) {
        override val collectionName: String = "simpleDocs"
    }

    val id1 = ObjectId()
    val id2 = ObjectId()
    val id3 = ObjectId()

    private val storeThreeKV = mongoOperation {
        keyValueStoreTable.insertOne(KeyValueStore(id1, "first", 0.0))
        keyValueStoreTable.insertOne(KeyValueStore(id2, "second", 1.0))
        keyValueStoreTable.insertOne(KeyValueStore(id3, "third", 2.0))
    }.ignoreValue()

    fun queryByKey(id: ObjectId) = mongoOperation {
        keyValueStoreTable.findById(id)
    }

    @Test
    fun `query by mongo id`() {

        val res = localMongo(storeThreeKV + queryByKey(id2))

        expectThat(res.expectSuccess()?.description).isEqualTo("second")
    }

    @Test
    fun `mongo id must be unique`() {
        val uniqueId = ObjectId()

        val firstTime = mongoOperation {
            keyValueStoreTable.insertOne(KeyValueStore(uniqueId, "first", 0.0))
        }
        val objId = localMongo(firstTime).expectSuccess()

        val updateAgain = mongoOperation {
            keyValueStoreTable.insertOne(KeyValueStore(uniqueId, "second", 1.0))
            keyValueStoreTable.insertOne(KeyValueStore(uniqueId, "third", 2.0))
        }

        val fail = localMongo(updateAgain).expectFailure()
        expectThat(fail.msg).contains("duplicate key error")

        val res = localMongo(queryByKey(uniqueId))

        expectThat(res.expectSuccess()?.description).isEqualTo("first")
    }

    @Test
    fun `store objectId`() {
        val uniqueId = ObjectId()

        val storeOne = mongoOperation {
            keyValueStoreTable.insertOne(KeyValueStore(uniqueId, "first", 0.0))
        }
        val objId = localMongo(storeOne).expectSuccess()

        expectThat(objId?.asObjectId()?.value).isEqualTo(uniqueId)

    }
}