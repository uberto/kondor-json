package com.ubertob.kondor.json

import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ProfunctorTest {


    @Test
    fun `dimap maps the Product to an Int both ways`() {

        val productClone = JProduct.dimap(
            { pnum: Int -> Product(pnum, "Clone", "Not very interesting product", 100.0) },
            { product: Product -> product.id }
        )

        val json = productClone.render(123)

        expectThat(json).isEqualTo("""{"id": 123, "long_description": "Not very interesting product", "short-desc": "Clone", "price": 100.0}""")

        val prodNum = productClone.parse(json).expectSuccess()

        expectThat(prodNum).isEqualTo(123)

    }

    @Test
    fun `lmap maps the Product to an Int but only in render`() {

        val productClone =
            JProduct.lmap { pnum: Int -> Product(pnum, "Clone", "Not very interesting product", 100.0) }

        val json = productClone.render(124)

        expectThat(json).isEqualTo("""{"id": 124, "long_description": "Not very interesting product", "short-desc": "Clone", "price": 100.0}""")

        val product = productClone.parse(json).expectSuccess()

        expectThat(product).isEqualTo(Product(124, "Clone", "Not very interesting product", 100.0))

    }

    @Test
    fun `rmap maps the Product to an Int only in parsing`() {

        val productClone = JProduct.rmap { product: Product -> product.id }

        val json = productClone.render(Product(125, "Clone", "Not very interesting product", 100.0))

        expectThat(json).isEqualTo("""{"id": 125, "long_description": "Not very interesting product", "short-desc": "Clone", "price": 100.0}""")

        val product = productClone.parse(json).expectSuccess()

        expectThat(product).isEqualTo(125)

    }

}