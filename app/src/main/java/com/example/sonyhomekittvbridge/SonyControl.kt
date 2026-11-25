package com.example.sonyhomekittvbridge

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SonyControl {
    private val irccCodes = mapOf(
        "UP" to "AAAAAQAAAAEAAAB0Aw==",
        "DOWN" to "AAAAAQAAAAEAAAB1Aw==",
        "LEFT" to "AAAAAQAAAAEAAAA0Aw==",
        "RIGHT" to "AAAAAQAAAAEAAAAzAw==",
        "SELECT" to "AAAAAQAAAAEAAABlAw==",
        "BACK" to "AAAAAgAAAJcAAAAjAw==",
        "EXIT" to "AAAAAgAAAJcAAAAjAw==",
        "PLAY" to "AAAAAgAAAJcAAAAaAw==",
        "PAUSE" to "AAAAAgAAAJcAAAAZAw==",
        "STOP" to "AAAAAgAAAJcAAAAYAw==",
        "REWIND" to "AAAAAgAAAJcAAAAbAw==",
        "FAST_FORWARD" to "AAAAAgAAAJcAAAAcAw==",
        "NEXT" to "AAAAAgAAAJcAAAA9Aw==",
        "PREVIOUS" to "AAAAAgAAAJcAAAA8Aw==",
        "INFO" to "AAAAAQAAAAEAAAA6Aw==",
        "HOME" to "AAAAAQAAAAEAAABgAw==",
        "VOLUME_UP" to "AAAAAQAAAAEAAAASAw==",
        "VOLUME_DOWN" to "AAAAAQAAAAEAAAATAw==",
        "MUTE" to "AAAAAQAAAAEAAAAUAw==",
        "CHANNEL_UP" to "AAAAAQAAAAEAAAAQAw==",
        "CHANNEL_DOWN" to "AAAAAQAAAAEAAAARAw==",
        "INPUT" to "AAAAAQAAAAEAAAAlAw==",
        "NUM_0" to "AAAAAQAAAAEAAAAJAw==",
        "NUM_1" to "AAAAAQAAAAEAAAAAAw==",
        "NUM_2" to "AAAAAQAAAAEAAAABAw==",
        "NUM_3" to "AAAAAQAAAAEAAAACAw==",
        "NUM_4" to "AAAAAQAAAAEAAAADAw==",
        "NUM_5" to "AAAAAQAAAAEAAAAEAw==",
        "NUM_6" to "AAAAAQAAAAEAAAAFAw==",
        "NUM_7" to "AAAAAQAAAAEAAAAGAw==",
        "NUM_8" to "AAAAAQAAAAEAAAAHAw==",
        "NUM_9" to "AAAAAQAAAAEAAAAIAw=="
    )

    fun sendRemoteKey(host: String, psk: String, key: String) {
        val code = irccCodes[key] ?: error("Tecla no compatible: $key")
        val xml = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body><u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1"><IRCCCode>$code</IRCCCode></u:X_SendIRCC></s:Body>
</s:Envelope>"""
        val connection = (URL("http://$host/sony/IRCC").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "text/xml; charset=UTF-8")
            setRequestProperty("SOAPAction", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
            if (psk.isNotBlank()) setRequestProperty("X-Auth-PSK", psk)
        }
        try {
            connection.outputStream.use { it.write(xml.toByteArray(Charsets.UTF_8)) }
            val codeStatus = connection.responseCode
            if (codeStatus !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                error("Sony IRCC HTTP $codeStatus: $body")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun getPowerStatus(host: String, psk: String): Boolean {
        val response = call(host, psk, "system", "getPowerStatus", JSONArray())
        val result = response.getJSONArray("result").getJSONArray(0).getJSONObject(0)
        return result.optString("status").equals("active", true)
    }

    fun setPowerStatus(host: String, psk: String, on: Boolean) {
        call(host, psk, "system", "setPowerStatus",
            JSONArray().put(JSONObject().put("status", on)))
    }

    fun call(host: String, psk: String, service: String, method: String, params: JSONArray): JSONObject {
        val connection = (URL("http://$host/sony/$service").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (psk.isNotBlank()) setRequestProperty("X-Auth-PSK", psk)
        }
        try {
            val body = JSONObject().put("method", method).put("params", params)
                .put("id", 1).put("version", "1.0").toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) error("Sony HTTP $code: $text")
            val json = JSONObject(text)
            if (json.has("error")) error("Sony API: ${json.optJSONArray("error")}")
            return json
        } finally {
            connection.disconnect()
        }
    }
}
