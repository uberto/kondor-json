package com.ubertob.kondor.loadTests

import com.ubertob.kondor.json.InvoiceId
import com.ubertob.kondor.json.JInvoice
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.randomInvoice
import com.ubertob.kondor.outcome.recover
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ParallelTest {

    @Test
    fun `parse multi threads`() {
        parallel { thread ->
            var res = ""
            repeat(100) { iter ->
                val invoice = randomInvoice().copy(id = InvoiceId("t:$thread-$iter"))
                val json = JInvoice.toJson(invoice)

                KondorTokenizer.tokenize(json)
                res += JInvoice.fromJson(json).transform {
                    if (it == invoice) "" else "diff! ${invoice.id}"
                }.recover { it.msg }
           }

            res

        }
    }


    fun parallel(fn: (Int) -> String) {
        val executor = Executors.newFixedThreadPool(50) // Create a thread pool with 50 threads
        val futures = mutableListOf<Future<String?>>()

        for (i in 1..50) {
            val future = executor.submit { fn(i) } as Future<String?>
            futures.add(future)
        }

        val results = futures.map { it.get() }.filterNotNull()
        executor.shutdown()

        expectThat(results).isEqualTo(emptyList())

    }
}