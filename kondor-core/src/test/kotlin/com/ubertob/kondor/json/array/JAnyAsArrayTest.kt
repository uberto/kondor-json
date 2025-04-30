import com.ubertob.kondor.json.JInt
import com.ubertob.kondor.json.array.JAnyAsArray
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JAnyAsArrayTest {
    @Test
    fun `encodes an object that may be represented as an array`() {
        val a = Arrayable(listOf(1, 2, 3))
        val json = JArrayable.toJson(a)

        expectThat(json).isEqualTo("[1, 2, 3]")
        expectThat(JArrayable.fromJson(json).expectSuccess()).isEqualTo(a)
    }
}

private data class Arrayable(val list: List<Int>)

private val JArrayable = JAnyAsArray(JInt, ::Arrayable, Arrayable::list)
