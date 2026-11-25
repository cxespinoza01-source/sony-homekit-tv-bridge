package com.example.sonyhomekittvbridge

import android.content.Context
import android.net.wifi.WifiManager
import io.github.hapjava.server.HomekitAccessoryCategories
import io.github.hapjava.server.impl.HomekitRoot
import io.github.hapjava.server.impl.HomekitServer
import java.net.InetAddress
import javax.jmdns.JmDNS

class HomeKitBridgeManager(private val context: Context) {
    private var server: HomekitServer? = null
    private var root: HomekitRoot? = null
    private var jmdns: JmDNS? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    @Synchronized
    fun start(host: String, psk: String): String {
        if (root != null) return "HomeKit ya está iniciado."
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifi.connectionInfo
        val ip = info.ipAddress
        if (ip == 0) error("Conecta el televisor a la red Wi-Fi antes de iniciar HomeKit.")
        val address = InetAddress.getByAddress(byteArrayOf(
            (ip and 0xff).toByte(), (ip shr 8 and 0xff).toByte(),
            (ip shr 16 and 0xff).toByte(), (ip shr 24 and 0xff).toByte()
        ))
        multicastLock = wifi.createMulticastLock("SonyHomeKitBridge").apply {
            setReferenceCounted(false)
            acquire()
        }
        try {
            jmdns = JmDNS.create(address)
            val auth = AndroidHomekitAuthInfo(context.applicationContext)
            server = HomekitServer(jmdns, 9123)
            root = server!!.createBridge(
                auth, "Sony BRAVIA", HomekitAccessoryCategories.TELEVISION,
                "Sony", "XBR-49X839C", "XBR-49X839C-Bridge", "0.2.0-alpha", "1"
            ).also {
                it.addAccessory(SonyHomekitTelevision(SonyControl(), host, psk))
                it.start()
            }
            return "Servidor HomeKit iniciado. Código de Casa: ${auth.pin}"
        } catch (e: Exception) {
            stop()
            throw e
        }
    }

    @Synchronized
    fun stop() {
        try { root?.stop() } catch (_: Exception) {}
        try { server?.stop() } catch (_: Exception) {}
        try { jmdns?.close() } catch (_: Exception) {}
        try { multicastLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        root = null
        server = null
        jmdns = null
        multicastLock = null
    }
}
