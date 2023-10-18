package com.ubertob.kondor.json

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole


@State(Scope.Benchmark)
open class FileInfoFixtures {

    val jFileInfos = JList(JFileInfo)

    val fileInfos = (1..100).map {
        randomFileInfo().copy(name = it.toString())
    }.toList()

    val jsonString = jFileInfos.toJson(fileInfos)
}

open class KondorBenchmark {

    @Benchmark
    fun simpleJsonSerialization(blackHole: Blackhole, fif: FileInfoFixtures) {
        with(fif) {
            val json = jFileInfos.toJson(fileInfos)
            blackHole.consume(json)
        }
    }


    @Benchmark
    fun simpleJsonDeserialization(blackHole: Blackhole, fif: FileInfoFixtures) =
        with(fif) {
            val list = jFileInfos.fromJson(jsonString)
            blackHole.consume(list)
        }

}
