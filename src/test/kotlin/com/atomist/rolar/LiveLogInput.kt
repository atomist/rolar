package com.atomist.rolar

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.json.JSONObject
import java.net.InetAddress
import java.util.*

fun main(args : Array<String>) {

    val mapper = jacksonObjectMapper()
    val testName = "loadTest/claymccoy/4"
    val hostName = InetAddress.getLocalHost().hostName
    val count = 1000
    val gmtFormat = LogKey.constructGmtFormat()
    for (i in 0 .. count) {
        val now = Date()
        val log = IncomingLog(
                host = hostName,
                content = listOf(
                        LogLine(
                                level = "info",
                                message = "test $i",
                                timestamp = null,
                                timestampMillis = now.time
                        )
                )
        )
        println(log)
        val closingSuffix = if (i < count) "" else "?closed=true"
        khttp.post(url = "http://localhost:8080/api/logs/$testName$closingSuffix",
                headers = mapOf("Content-Type" to "application/json"),
                json = JSONObject(mapper.writeValueAsString(log))
        )
    }

}


