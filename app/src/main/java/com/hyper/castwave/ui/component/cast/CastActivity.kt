package com.hyper.castwave.ui.component.cast

import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.banner_all
import com.hyper.castwave.ads.inter_back
import com.hyper.castwave.ui.bases.ext.animateClick
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hyper.castwave.R
import com.hyper.castwave.navigation.AppNavigator
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.databinding.CastActivityCastBinding
import com.hyper.castwave.domain.model.CastDevice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CastActivity : BaseActivity<CastActivityCastBinding>() {

    private val viewModel: CastViewModel by viewModels()
    @Inject
    lateinit var navigator: AppNavigator

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    private val deviceAdapter = DeviceAdapter(
        onDeviceClick = { device ->
            if (device.isConnected) {
                viewModel.connectDevice(device.id)
                viewModel.disconnect()
                shouldDisconnectAfterConnect = true
            } else {
                if (isFromMediaPicker) {
                    viewModel.connectDevice(device.id)
                } else {
                    openCastSettings()
                }
            }
        },
        onDeviceLongClick = { device -> viewModel.requestPinForDevice(device.id) },
    )

    fun loadBanner() {
        AdsManager.loadBanner(this, AdRemoteConfig.banner_all, mBinding.frAdsBanner, false)
    }

    private var pinDialog: PinDialog? = null
    private var shouldDisconnectAfterConnect = false

    private val isFromMediaPicker: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_MEDIA_PICKER, false)
    }

    override fun getLayoutActivity(): Int = R.layout.cast_activity_cast

    override fun initViews() {
        super.initViews()
        applyCastSystemBars()
        networkMonitor.attach()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) = CastActivityCastBinding.inflate(inflater)
    private fun stateFlow() = viewModel.uiState

    private fun setupViews() {
        loadBanner()
        mBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerView.adapter = deviceAdapter
        mBinding.recyclerView.itemAnimator = null
    }

    private fun applyCastSystemBars() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.cast_cast_background_start)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupClicks() {
        mBinding.rlBrowserCast.setOnClickListener {
            navigator.openWebCast(this)        }
        mBinding.btnBack.animateClick {
            AdsManager.loadAndShowInterstitial(this@CastActivity, AdRemoteConfig.inter_back) {
                onBackPressed()
            }
        }

        mBinding.btnRefresh.setOnClickListener {
            val icon = mBinding.imgRefresh
            icon.animate().cancel()
            icon.rotation = 0f
            icon.animate()
                .rotationBy(360f)
                .setDuration(500)
                .setInterpolator(LinearInterpolator())
                .start()
            viewModel.refresh()
        }

        mBinding.tvActionError.setOnClickListener {
            when (viewModel.uiState.value) {
                CastUiState.Empty -> viewModel.refresh()
                CastUiState.LostConnection -> openWifiSettings()
                else -> {}
            }
        }

        mBinding.btnOpenWirelessDisplay.setOnClickListener {
            openCastSettings()
        }
    }

    private fun renderState(state: CastUiState) {
        if (state !is CastUiState.PinRequired) dismissPinDialog()

        when (state) {
            CastUiState.Loading -> transitionToLoading()
            is CastUiState.HasDevices -> transitionToDevices(state)
            CastUiState.Empty -> transitionToEmpty()
            CastUiState.LostConnection -> transitionToLostConnection()
            is CastUiState.PinRequired -> showPinDialog(state.device)
            is CastUiState.Connected -> {
                if (shouldDisconnectAfterConnect) {
                    shouldDisconnectAfterConnect = false
                    viewModel.disconnect()
                    return
                }

                transitionToDevices(
                    CastUiState.HasDevices(
                        devices = state.devices,
                        selectedId = state.connectedDevice.id,
                        connectingId = null
                    )
                )

                if (isFromMediaPicker) {
                    finish()
                }
            }
        }
    }

    private fun transitionToLoading() {
        mBinding.tvSearchingTitle.text = getString(R.string.cast_cast_searching_devices)
        fadeIn(mBinding.imgSearching, mBinding.tvSearchingTitle, mBinding.tvSearchingSubtitle)
        fadeOut(mBinding.tvStatus, mBinding.recyclerView, mBinding.emptyDeviceContainer)
    }

    private fun transitionToDevices(state: CastUiState.HasDevices) {
        val newConnectingId = state.connectingId
        deviceAdapter.selectedId = state.selectedId
        deviceAdapter.submitList(state.devices) {
            deviceAdapter.connectingId = newConnectingId
        }
        mBinding.tvStatus.text = getString(R.string.cast_cast_available_devices)
        fadeIn(mBinding.imgSearching)
        fadeIn(mBinding.tvSearchingTitle, mBinding.tvSearchingSubtitle)
        fadeOut(mBinding.emptyDeviceContainer)
        fadeIn(mBinding.tvStatus, mBinding.recyclerView)
    }

    private fun transitionToEmpty() {
        mBinding.imgNoDevice.setImageResource(R.drawable.cast_img_state_no_device)
        mBinding.tvTitleError.text = getString(R.string.cast_home_no_devices)
        mBinding.tvMessageError.text = getString(R.string.cast_title_br1)
        mBinding.tvActionErrorLabel.text = getString(R.string.cast_refresh)
        fadeOut(mBinding.imgSearching, mBinding.tvSearchingTitle, mBinding.tvSearchingSubtitle)
        fadeOut(mBinding.tvStatus, mBinding.recyclerView)
        fadeIn(mBinding.emptyDeviceContainer)
    }

    private fun transitionToLostConnection() {
        mBinding.imgNoDevice.setImageResource(R.drawable.cast_img_state_lost_connection)
        mBinding.tvTitleError.text = getString(R.string.cast_cast_lost_connection)
        mBinding.tvMessageError.text = getString(R.string.cast_title_br1)
        mBinding.tvActionErrorLabel.text = getString(R.string.cast_open_wifi_settings)
        fadeOut(mBinding.imgSearching, mBinding.tvSearchingTitle, mBinding.tvSearchingSubtitle)
        fadeOut(mBinding.tvStatus, mBinding.recyclerView)
        fadeIn(mBinding.emptyDeviceContainer)
    }

    private fun showPinDialog(device: CastDevice) {
        if (pinDialog?.isShowing == true) return
        pinDialog = PinDialog(
            context = this,
            deviceName = device.name,
            onConfirm = { pin -> viewModel.submitPin(pin) },
            onCancel = { viewModel.cancelPin() },
        ).also { it.show() }
    }

    private fun dismissPinDialog() {
        pinDialog?.dismiss()
        pinDialog = null
    }

    private fun fadeIn(vararg views: View) {
        views.forEach { view ->
            view.animate().cancel()
            if (view.visibility != View.VISIBLE) {
                view.alpha = 0f
                view.visibility = View.VISIBLE
            }
            view.animate().alpha(1f).setDuration(220).start()
        }
    }

    private fun fadeOut(vararg views: View) {
        views.forEach { view ->
            view.animate().cancel()
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.GONE
                return@forEach
            }
            view.animate().alpha(0f).setDuration(160).withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
            }.start()
        }
    }

    private fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun openCastSettings() {
        try {
            startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
        } catch (e2: Exception) {
            e2.printStackTrace()
            try {
                startActivity(Intent("android.settings.CAST_SETTINGS"))
            } catch (e3: Exception) {
                e3.printStackTrace()
            }
        }
    }

    companion object {
        const val EXTRA_FROM_MEDIA_PICKER = "extra_from_media_picker"
    }
}
