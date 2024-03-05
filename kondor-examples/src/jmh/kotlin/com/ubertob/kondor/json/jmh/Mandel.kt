package com.ubertob.kondor.json.jmh

import com.ubertob.kondortools.chronoAndLog


//a Mandlebrot Set generator to test stuff in inner loops
fun main() {
    val width = 150
    val height = 40
    val maxIter = 256
    val zoom = 0.5


    println("Printing Mandel with zoom $zoom")

    repeat(1) {
//        System.gc()

        chronoAndLog("mandel") {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val zx =   1.0*x / width -0.75
                    val zy =   1.0*y / height -0.5

                    var cX = zx / zoom
                    var cY = zy / zoom
                    var iter = maxIter
                    var zX = 0.0
                    var zY = 0.0
                    while (zX * zX + zY * zY < 4.0 && iter > 0) {
                        val tmp = zX * zX - zY * zY + cX

                        zY = 2.0 * zX * zY + cY
                        zX = tmp
                        iter--
                     
                    }
                    when {
                        iter > 250 -> print(".")
                        iter > 245 -> print("o")
                        iter > 230 -> print("*")
                        iter > 150 -> print("#")
                        else -> print(" ")
                    }
                }
                println()
            }
        }
    }
}