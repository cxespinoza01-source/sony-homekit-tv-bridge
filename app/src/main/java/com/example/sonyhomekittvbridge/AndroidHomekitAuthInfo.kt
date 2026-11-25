package com.example.sonyhomekittvbridge

import android.content.Context
import android.util.Base64
import io.github.hapjava.server.HomekitAuthInfo
import io.github.hapjava.server.impl.HomekitServer
import java.math.BigInteger

class AndroidHomekitAuthInfo(context: Context) : HomekitAuthInfo {
    private val prefs = context.getSharedPreferences("homekit_pairing", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains("pin")) {
            val key = HomekitServer.generateKey()
            prefs.edit()
                .putString("pin", HomekitServer.generatePin())
                .putString("mac", HomekitServer.generateMac())
                .putString("salt", HomekitServer.generateSalt().toString())
                .putString("private_key", Base64.encodeToString(key, Base64.NO_WRAP))
                .putString("setup_id", "SNY1")
                .apply()
        }
    }

    override fun getPin(): String = prefs.getString("pin", "482-73-619")!!
    override fun getSetupId(): String = prefs.getString("setup_id", "SNY1")!!
    override fun getMac(): String = prefs.getString("mac", "")!!
    override fun getSalt(): BigInteger = BigInteger(prefs.getString("salt", "1")!!)
    override fun getPrivateKey(): ByteArray =
        Base64.decode(prefs.getString("private_key", "")!!, Base64.NO_WRAP)

    override fun createUser(username: String, publicKey: ByteArray, isAdmin: Boolean) {
        prefs.edit()
            .putString("user_$username", Base64.encodeToString(publicKey, Base64.NO_WRAP))
            .putBoolean("admin_$username", isAdmin)
            .apply()
    }

    override fun removeUser(username: String) {
        prefs.edit().remove("user_$username").remove("admin_$username").apply()
    }

    override fun getUserPublicKey(username: String): ByteArray? {
        val value = prefs.getString("user_$username", null) ?: return null
        return Base64.decode(value, Base64.NO_WRAP)
    }

    override fun listUsers(): MutableCollection<String> =
        prefs.all.keys.filter { it.startsWith("user_") }.map { it.removePrefix("user_") }.toMutableList()

    override fun userIsAdmin(username: String): Boolean =
        prefs.getBoolean("admin_$username", false)

    override fun hasUser(): Boolean = listUsers().isNotEmpty()
}
