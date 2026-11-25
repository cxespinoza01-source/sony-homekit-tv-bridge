package com.example.sonyhomekittvbridge

import io.github.hapjava.accessories.TelevisionAccessory
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback
import io.github.hapjava.characteristics.impl.television.RemoteKeyEnum
import io.github.hapjava.characteristics.impl.television.SleepDiscoveryModeEnum
import java.util.concurrent.CompletableFuture

class SonyHomekitTelevision(
    private val sony: SonyControl,
    private val host: String,
    private val psk: String
) : TelevisionAccessory {
    @Volatile private var activeCallback: HomekitCharacteristicChangeCallback? = null
    @Volatile private var inputCallback: HomekitCharacteristicChangeCallback? = null
    @Volatile private var nameCallback: HomekitCharacteristicChangeCallback? = null

    override fun getId() = 2
    override fun getName() = CompletableFuture.completedFuture("Sony BRAVIA")
    override fun getSerialNumber() = CompletableFuture.completedFuture("XBR-49X839C")
    override fun getModel() = CompletableFuture.completedFuture("XBR-49X839C")
    override fun getManufacturer() = CompletableFuture.completedFuture("Sony")
    override fun getFirmwareRevision() = CompletableFuture.completedFuture("Bridge alpha")
    override fun identify() {}

    override fun isActive(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync { sony.getPowerStatus(host, psk) }

    override fun setActive(state: Boolean): CompletableFuture<Void> =
        CompletableFuture.runAsync { sony.setPowerStatus(host, psk, state) }

    override fun subscribeActive(callback: HomekitCharacteristicChangeCallback) { activeCallback = callback }
    override fun unsubscribeActive() { activeCallback = null }

    override fun getActiveIdentifier(): CompletableFuture<Int> = CompletableFuture.completedFuture(1)
    override fun setActiveIdentifier(identifier: Int): CompletableFuture<Void> = CompletableFuture.completedFuture(null)
    override fun subscribeActiveIdentifier(callback: HomekitCharacteristicChangeCallback) { inputCallback = callback }
    override fun unsubscribeActiveIdentifier() { inputCallback = null }

    override fun getConfiguredName(): CompletableFuture<String> = CompletableFuture.completedFuture("Sony BRAVIA")
    override fun setConfiguredName(name: String): CompletableFuture<Void> = CompletableFuture.completedFuture(null)
    override fun subscribeConfiguredName(callback: HomekitCharacteristicChangeCallback) { nameCallback = callback }
    override fun unsubscribeConfiguredName() { nameCallback = null }

    override fun setRemoteKey(key: RemoteKeyEnum): CompletableFuture<Void> =
        CompletableFuture.completedFuture(null)

    override fun getSleepDiscoveryMode(): CompletableFuture<SleepDiscoveryModeEnum> =
        CompletableFuture.completedFuture(SleepDiscoveryModeEnum.ALWAYS_DISCOVERABLE)

    override fun subscribeSleepDiscoveryMode(callback: HomekitCharacteristicChangeCallback) {}
    override fun unsubscribeSleepDiscoveryMode() {}
}
