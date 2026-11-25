package com.example.sonyhomekittvbridge

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SonyControl {
    fun getPowerStatus(host: String, psk: String): Boolean {
        val response = call(host, psk, "system", "getPowerStatus", JSONArray())
        val result = response.getJSONArray("result").getJSONArray(0).getJSONObject(0)
        return result.optString("status").equals("active", true)
    }

    fun setPowerStatus(host: String, psk: String, on: Boolean) {
        call(host, psk, "system", "setPowerStatus",
            JSONArray().put(JSONObject().put("status", on)))
    }

    private fun call(host: String, psk: String, service: String, method: String, params: JSONArray): JSONObject {
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
