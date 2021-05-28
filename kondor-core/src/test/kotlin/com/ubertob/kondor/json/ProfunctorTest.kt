package com.ubertob.kondor.json

import com.ubertob.kondor.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ProfunctorTest {

    @Test
    fun `create a product clone`() {
        val productClone = JProduct.asProfunctor().dimap(
            { pnum: Int -> Product(pnum, "Clone", "Not very interesting product", 100.0) },
            { product: Product -> product.id }
        )

        val json = productClone.render(123)

        expectThat(json).isEqualTo("""{"id": 123, "long_description": "Not very interesting product", "short-desc": "Clone", "price": 100.0}""")

        val prodNum = productClone.parse(json).expectSuccess()

        expectThat(prodNum).isEqualTo(123)

    }



}