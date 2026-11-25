package com.example.sonyhomekittvbridge

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.util.Log
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
    private lateinit var homeKitManager: HomeKitBridgeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeKitManager = HomeKitBridgeManager(applicationContext)

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
            text = "Sony HomeKit TV Bridge\nVersión 0.2.0-alpha"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        content.addView(heading)

        status = TextView(this).apply {
            text = "Control local Sony y puente HomeKit para Apple Casa."
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
        addAction(content, "Volumen +1") { changeVolume(1) }
        addAction(content, "Volumen -1") { changeVolume(-1) }
        addAction(content, "Iniciar servidor HomeKit") {
            val host = ipField.text.toString().trim()
            val key = pskField.text.toString()
            status.text = "Iniciando HomeKit y anunciando el puente…"
            executor.execute {
                val result = try {
                    homeKitManager.start(host, key)
                } catch (error: Throwable) {
                    Log.e("SonyHomeKitBridge", "Fallo al iniciar el servidor HomeKit", error)
                    val detail = error.localizedMessage ?: error.javaClass.name
                    "Error HomeKit (" + error.javaClass.simpleName + "): " + detail
                }
                runOnUiThread { status.text = result }
            }
        }
        addAction(content, "Detener servidor HomeKit") {
            homeKitManager.stop()
            status.text = "Servidor HomeKit detenido."
        }

        val note = TextView(this).apply {
            text = "Para añadirlo: inicia el servidor HomeKit, abre Casa en tu iPhone, toca + > Añadir accesorio > Más opciones y selecciona Sony BRAVIA. Introduce el código que aparece aquí. Mantén esta app abierta y el TV encendido. No compartas ni publiques tu PSK ni el código de emparejamiento."
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

    private fun changeVolume(delta: Int) {
        val host = ipField.text.toString().trim()
        val key = pskField.text.toString()
        if (host.isBlank()) {
            status.text = "Introduce la IP del televisor."
            return
        }
        status.text = "Consultando volumen…"
        executor.execute {
            val result = try {
                val currentInfo = sonyCall(host, key, "getVolumeInformation", JSONArray())
                val entries = currentInfo.getJSONArray("result").getJSONArray(0)
                var speaker: JSONObject? = null
                for (i in 0 until entries.length()) {
                    val item = entries.getJSONObject(i)
                    if (item.optString("target") == "speaker") {
                        speaker = item
                        break
                    }
                }
                val info = speaker ?: throw IllegalStateException("El TV no informó el volumen de los altavoces.")
                val current = info.getInt("volume")
                val min = info.optInt("minVolume", 0)
                val max = info.optInt("maxVolume", 100)
                val next = (current + delta).coerceIn(min, max)
                if (next == current) {
                    "Volumen sin cambios: ya está en el límite ($current)."
                } else {
                    val params = JSONArray().put(
                        JSONObject().put("volume", next.toString()).put("target", "speaker")
                    )
                    val setResponse = sonyCall(host, key, "setAudioVolume", params)
                    if (setResponse.has("error")) {
                        throw IllegalStateException("El TV rechazó el cambio: ${setResponse.optJSONArray("error")}")
                    }
                    val verify = sonyCall(host, key, "getVolumeInformation", JSONArray())
                    val verifiedEntries = verify.getJSONArray("result").getJSONArray(0)
                    var actual = next
                    for (i in 0 until verifiedEntries.length()) {
                        val item = verifiedEntries.getJSONObject(i)
                        if (item.optString("target") == "speaker") {
                            actual = item.getInt("volume")
                            break
                        }
                    }
                    "Volumen solicitado: $next. Volumen informado por el TV: $actual."
                }
            } catch (error: Exception) {
                "No se pudo cambiar el volumen: ${error.localizedMessage ?: error.javaClass.simpleName}"
            }
            runOnUiThread { status.text = result }
        }
    }

    private fun sonyCall(
        host: String,
        key: String,
        method: String,
        params: JSONArray
    ): JSONObject {
        val connection = (URL("http://$host/sony/audio").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (key.isNotBlank()) setRequestProperty("X-Auth-PSK", key)
        }
        try {
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
            if (code !in 200..299) throw IllegalStateException("HTTP $code: $response")
            val json = JSONObject(response)
            if (json.has("error")) throw IllegalStateException("Error Sony: ${json.optJSONArray("error")}")
            return json
        } finally {
            connection.disconnect()
        }
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
        homeKitManager.stop()
        executor.shutdownNow()
        super.onDestroy()
    }
}
