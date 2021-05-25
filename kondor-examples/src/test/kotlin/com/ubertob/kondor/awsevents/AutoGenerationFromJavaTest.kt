package com.ubertob.kondor.awsevents

import com.ubertob.kondortools.generateConverterFileFor

class AutoGenerationFromJavaTest {


}

fun main() {

    println(generateConverterFileFor(APIGatewayProxyRequestEvent::class))


}