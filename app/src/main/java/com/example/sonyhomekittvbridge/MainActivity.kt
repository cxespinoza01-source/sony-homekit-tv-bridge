package com.example.sonyhomekittvbridge

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var ipField: EditText
    private lateinit var pskField: EditText
    private lateinit var status: TextView
    private var muted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 20, 28, 20)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val heading = TextView(this).apply {
            text = "Sony HomeKit TV Bridge\nVersión 0.1.0-alpha"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        content.addView(heading)

        status = TextView(this).apply {
            text = "Prueba de control local Sony.\nHomeKit/HAP aún no está implementado."
            textSize = 16f
            setPadding(0, 8, 0, 18)
        }
        content.addView(status)

        ipField = EditText(this).apply {
            hint = "IP del televisor"
            setSingleLine(true)
            setText("192.168.100.40")
            textSize = 18f
        }
        content.addView(ipField, matchWidth())

        pskField = EditText(this).apply {
            hint = "Clave PSK (se queda en este dispositivo)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            textSize = 18f
        }
        content.addView(pskField, matchWidth())

        addAction(content, "Probar conexión / información") {
            sonyRequest("system", "getSystemInformation", JSONArray())
        }
        addAction(content, "Consultar estado de energía") {
            sonyRequest("system", "getPowerStatus", JSONArray())
        }
        addAction(content, "Encender") {
            sonyRequest("system", "setPowerStatus", JSONArray().put(JSONObject().put("status", true)))
        }
        addAction(content, "Apagar") {
            sonyRequest("system", "setPowerStatus", JSONArray().put(JSONObject().put("status", false)))
        }
        addAction(content, "Silenciar / activar sonido") {
            muted = !muted
            sonyRequest("audio", "setAudioMute", JSONArray().put(JSONObject().put("status", muted)))
        }
        addAction(content, "Volumen +1") {
            sonyRequest("audio", "setAudioVolume", JSONArray().put(
                JSONObject().put("volume", "+1").put("ui", "on").put("target", "speaker")
            ))
        }
        addAction(content, "Volumen -1") {
            sonyRequest("audio", "setAudioVolume", JSONArray().put(
                JSONObject().put("volume", "-1").put("ui", "on").put("target", "speaker")
            ))
        }

        val note = TextView(this).apply {
            text = "Nota: esta alfa solo prueba comandos Sony por IP. No se puede añadir todavía a Apple Casa. No compartas ni publiques tu PSK."
            textSize = 14f
            setPadding(0, 18, 0, 8)
        }
        content.addView(note)

        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = label
            textSize = 17f
            setOnClickListener { action() }
        }, matchWidth())
    }

    private fun sonyRequest(service: String, method: String, params: JSONArray) {
        val host = ipField.text.toString().trim()
        val key = pskField.text.toString()
        if (host.isBlank()) {
            status.text = "Introduce la IP del televisor."
            return
        }
        status.text = "Enviando $method…"
        executor.execute {
            val result = try {
                val url = URL("http://$host/sony/$service")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    if (key.isNotBlank()) setRequestProperty("X-Auth-PSK", key)
                }
                val body = JSONObject()
                    .put("method", method)
                    .put("params", params)
                    .put("id", 1)
                    .put("version", "1.0")
                    .toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() } ?: "(sin cuerpo)"
                connection.disconnect()
                "HTTP $code\n$response"
            } catch (error: Exception) {
                "Error: ${error.localizedMessage ?: error.javaClass.simpleName}\nComprueba que el TV esté encendido, en la misma red y que la PSK sea correcta."
            }
            runOnUiThread { status.text = result }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
