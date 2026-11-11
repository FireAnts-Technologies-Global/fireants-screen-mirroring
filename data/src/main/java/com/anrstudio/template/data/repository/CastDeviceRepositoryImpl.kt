package com.hyper.castwave.data.repository

import android.content.Context
import android.util.Log
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider
import com.connectsdk.service.AirPlayService
import com.connectsdk.service.DIALService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.command.ServiceCommandError
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.hyper.castwave.domain.model.CastDevice
import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.DeviceType
import com.hyper.castwave.domain.repository.CastDeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CastDeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smartTvSessionStore: SmartTvSessionStore,
) : CastDeviceRepository {

    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private val mediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(
            CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
        )
        .build()

    private val mediaRouter: MediaRouter by lazy { MediaRouter.getInstance(context) }
    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }

    private var sessionListenerRegistered = false
    private var discoveryManagerInitialized = false
    private var connectSdkListenerRegistered = false
    private var smartTvDevices = emptyMap<String, SmartTvDevice>()

    private val routerCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) =
            refreshDevices()

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) =
            refreshDevices()

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) =
            refreshDevices()

        override fun onRouteSelected(
            router: MediaRouter,
            route: MediaRouter.RouteInfo,
            reason: Int
        ) = refreshDevices()

        override fun onRouteUnselected(
            router: MediaRouter,
            route: MediaRouter.RouteInfo,
            reason: Int
        ) = refreshDevices()
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _connectionState.value = ConnectionState.Connecting
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            refreshDevices()
            _connectionState.value = ConnectionState.Connected(session.toDomainDevice())
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            refreshDevices()
            _connectionState.value = if (error == CastStatusCodes.NOT_ALLOWED) {
                ConnectionState.PinRequired(mediaRouter.selectedRoute.toCastDevice())
            } else {
                ConnectionState.Disconnected
            }
        }

        override fun onSessionEnding(session: CastSession) {
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            refreshDevices()
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            refreshDevices()
            _connectionState.value = ConnectionState.Connected(session.toDomainDevice())
        }

        override fun onSessionResumeFailed(session: CastSession, errorCode: Int) {
            _connectionState.value = ConnectionState.Lost
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            refreshDevices()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val discoveryListener = object : DiscoveryManagerListener {
        override fun onDeviceAdded(manager: DiscoveryManager, device: ConnectableDevice) =
            refreshDevices()

        override fun onDeviceUpdated(manager: DiscoveryManager, device: ConnectableDevice) =
            refreshDevices()

        override fun onDeviceRemoved(manager: DiscoveryManager, device: ConnectableDevice) =
            refreshDevices()

        override fun onDiscoveryFailed(manager: DiscoveryManager, error: ServiceCommandError) =
            refreshDevices()
    }

    override fun observeDevices(): Flow<List<CastDevice>> = _devices.asStateFlow()

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun startDiscovery() {
        withContext(Dispatchers.Main) {
            if (!sessionListenerRegistered) {
                castContext.sessionManager.addSessionManagerListener(
                    sessionListener,
                    CastSession::class.java
                )
                sessionListenerRegistered = true
            }
            mediaRouter.addCallback(
                mediaRouteSelector,
                routerCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN,
            )
            startConnectSdkDiscovery()
            syncCurrentSessionState()
            refreshDevices()
        }
    }

    private fun startConnectSdkDiscovery() {
        if (!discoveryManagerInitialized) {
            DiscoveryManager.init(context)
            discoveryManagerInitialized = true
        }
        val discoveryManager = DiscoveryManager.getInstance()
        discoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON)
        runCatching {
            discoveryManager.unregisterDeviceService(
                AirPlayService::class.java,
                ZeroconfDiscoveryProvider::class.java
            )
            discoveryManager.unregisterDeviceService(
                DIALService::class.java,
                SSDPDiscoveryProvider::class.java
            )
        }
        if (!connectSdkListenerRegistered) {
            discoveryManager.addListener(discoveryListener)
            connectSdkListenerRegistered = true
        }
        discoveryManager.start()
    }

    private fun syncCurrentSessionState() {
        val currentSession = castContext.sessionManager.currentCastSession
        if (currentSession != null && currentSession.isConnected) {
            _connectionState.value = ConnectionState.Connected(currentSession.toDomainDevice())
        }
    }

    override suspend fun stopDiscovery() {
        withContext(Dispatchers.Main) {
            mediaRouter.removeCallback(routerCallback)
            if (discoveryManagerInitialized) DiscoveryManager.getInstance().stop()
        }
    }

    override suspend fun connect(deviceId: String): DataResult<Unit> {
        return withContext(Dispatchers.Main) {
            if (deviceId.startsWith(CONNECT_SDK_ID_PREFIX)) {
                val smartTvDevice = smartTvDevices[deviceId]
                    ?: return@withContext DataResult.Error(
                        IllegalArgumentException("Device not found"),
                        "Device not found",
                    )
                _connectionState.value = ConnectionState.Connecting
                return@withContext connectSmartTvDevice(smartTvDevice)
            }
            val route = mediaRouter.routes.find { it.id == deviceId }
                ?: run {
                    return@withContext DataResult.Error(
                        IllegalArgumentException("Device not found"),
                        "Device not found",
                    )
                }
            _connectionState.value = ConnectionState.Connecting
            mediaRouter.selectRoute(route)
            DataResult.Success(Unit)
        }
    }

    override suspend fun connectWithPin(deviceId: String, pin: String): DataResult<Unit> {
        return withContext(Dispatchers.Main) {
            val route = mediaRouter.routes.find { it.id == deviceId }
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Device not found"),
                    "Device not found",
                )
            _connectionState.value = ConnectionState.Connecting
            // Re-select route; Cast SDK handles PIN verification via its internal Guest Mode channel
            mediaRouter.selectRoute(route)
            DataResult.Success(Unit)
        }
    }

    override suspend fun requestPin(deviceId: String): DataResult<Unit> {
        return withContext(Dispatchers.Main) {
            val route = mediaRouter.routes.find { it.id == deviceId }
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Device not found"),
                    "Device not found",
                )
            _connectionState.value = ConnectionState.PinRequired(route.toCastDevice())
            DataResult.Success(Unit)
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.Main) {
            val smartTvDevice = smartTvSessionStore.selectedDevice
            smartTvSessionStore.selectedDevice = null
            runCatching { smartTvDevice?.device?.disconnect() }
            runCatching { castContext.sessionManager.endCurrentSession(true) }
            _connectionState.value = ConnectionState.Disconnected
            refreshDevices()
        }
    }


    private fun refreshDevices() {
        val castDevices = mediaRouter.routes
            .filter { !it.isDefault && it.matchesSelector(mediaRouteSelector) }
            .map { it.toCastDevice() }
        val connectSdkDevices = connectSdkDevices()
        val devices = (castDevices + connectSdkDevices).distinctBy { it.id }
        Log.d(
            TAG,
            "castDevices=${castDevices.size}, connectSdkDevices=${connectSdkDevices.size}, totalDevices=${devices.size}"
        )
        _devices.value = devices
    }

    private fun connectSdkDevices(): List<CastDevice> {
        if (!discoveryManagerInitialized) return emptyList()
        val smartDevices = DiscoveryManager.getInstance()
            .compatibleDevices
            .values
            .mapNotNull { device -> device.toSmartTvDeviceOrNull() }
        smartTvDevices = smartDevices.associateBy { it.id }
        return smartDevices.map { device ->
            CastDevice(
                id = device.id,
                name = device.name,
                modelName = device.modelName,
                isConnected = smartTvSessionStore.selectedDevice?.id == device.id || device.device.isConnected,
                type = DeviceType.CHROMECAST,
            )
        }
    }

    private fun ConnectableDevice.toSmartTvDeviceOrNull(): SmartTvDevice? {
        val deviceId = ipAddress ?: id ?: return null
        val deviceName = friendlyName ?: modelName ?: return null
        return SmartTvDevice(
            id = "$CONNECT_SDK_ID_PREFIX$deviceId",
            name = deviceName,
            modelName = connectedServiceNames ?: serviceId ?: "Smart TV",
            device = this,
        )
    }

    private suspend fun connectSmartTvDevice(smartTvDevice: SmartTvDevice): DataResult<Unit> {
        val device = smartTvDevice.device
        if (device.isConnected) {
            smartTvSessionStore.selectedDevice = smartTvDevice
            _connectionState.value =
                ConnectionState.Connected(smartTvDevice.toConnectedCastDevice())
            refreshDevices()
            return DataResult.Success(Unit)
        }
        return suspendCancellableCoroutine { continuation ->
            val listener = object : ConnectableDeviceListener {
                override fun onDeviceReady(connectableDevice: ConnectableDevice) {
                    connectableDevice.removeListener(this)
                    smartTvSessionStore.selectedDevice = smartTvDevice
                    _connectionState.value =
                        ConnectionState.Connected(smartTvDevice.toConnectedCastDevice())
                    refreshDevices()
                    continuation.resume(DataResult.Success(Unit))
                }

                override fun onDeviceDisconnected(connectableDevice: ConnectableDevice) = Unit

                override fun onPairingRequired(
                    connectableDevice: ConnectableDevice,
                    deviceService: DeviceService,
                    pairingType: DeviceService.PairingType,
                ) = Unit

                override fun onCapabilityUpdated(
                    connectableDevice: ConnectableDevice,
                    added: MutableList<String>,
                    removed: MutableList<String>,
                ) = Unit

                override fun onConnectionFailed(
                    connectableDevice: ConnectableDevice,
                    error: ServiceCommandError
                ) {
                    connectableDevice.removeListener(this)
                    _connectionState.value = ConnectionState.Disconnected
                    continuation.resume(
                        DataResult.Error(
                            RuntimeException(error.message),
                            error.message ?: "Device connection failed",
                        )
                    )
                }
            }
            device.addListener(listener)
            continuation.invokeOnCancellation { device.removeListener(listener) }
            device.connect()
        }
    }

    private fun SmartTvDevice.toConnectedCastDevice() = CastDevice(
        id = id,
        name = name,
        modelName = modelName,
        isConnected = true,
        type = DeviceType.CHROMECAST,
    )

    private fun MediaRouter.RouteInfo.toCastDevice() = CastDevice(
        id = id,
        name = name.toString(),
        modelName = description ?: "Cast Device",
        isConnected = connectionState == MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED,
        type = DeviceType.CHROMECAST,
    )

    private fun CastSession.toDomainDevice(): CastDevice {
        val googleDevice: com.google.android.gms.cast.CastDevice? = castDevice
        val route = mediaRouter.selectedRoute
        return CastDevice(
            id = route.id,
            name = googleDevice?.friendlyName ?: route.name.toString(),
            modelName = googleDevice?.modelName ?: "Cast Device",
            isConnected = true,
            type = DeviceType.CHROMECAST,
        )
    }

    private companion object {
        const val TAG = "CastDiscovery"
        const val CONNECT_SDK_ID_PREFIX = "connectsdk:"
    }
}
