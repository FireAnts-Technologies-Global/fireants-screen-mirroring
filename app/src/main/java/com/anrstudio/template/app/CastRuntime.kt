package com.anrstudio.template.app
import android.content.Context
import android.content.Intent
import com.anrstudio.template.data.server.UnifiedMediaServer
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.usecase.ObserveConnectionStateUseCase
import com.anrstudio.template.casting.service.CastForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class CastRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val server: UnifiedMediaServer,
    private val observeConnection: ObserveConnectionStateUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false
    fun start() {
        if (started) return
        started = true
        server.start()
        scope.launch {
            observeConnection().collect { state ->
                when (state) {
                    is ConnectionState.Connected -> context.startForegroundService(
                        Intent(context, CastForegroundService::class.java)
                            .putExtra(CastForegroundService.EXTRA_DEVICE_NAME, state.device.name))
                    is ConnectionState.Disconnected, is ConnectionState.Lost ->
                        context.stopService(Intent(context, CastForegroundService::class.java))
                    else -> Unit
                }
            }
        }
    }
}
