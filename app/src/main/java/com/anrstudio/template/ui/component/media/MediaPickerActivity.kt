package com.anrstudio.template.ui.component.media

import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdRemoteConfig
import com.anrstudio.template.ads.banner_all
import com.anrstudio.template.ads.inter_back
import com.anrstudio.template.ui.bases.ext.animateClick
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.anrstudio.template.R
import com.anrstudio.template.navigation.AppNavigator
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.databinding.CastActivityMediaPickerBinding
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.ui.component.dialog.LoadingDialog
import com.anrstudio.template.ui.component.cast.CastUiState
import com.anrstudio.template.ui.component.cast.CastViewModel
import com.anrstudio.template.ui.component.cast.DeviceAdapter
import com.anrstudio.template.ui.component.cast.PinDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.view.isVisible

@AndroidEntryPoint
class MediaPickerActivity : BaseActivity<CastActivityMediaPickerBinding>() {

    private val networkMonitor by lazy { com.anrstudio.template.utils.CastNetworkMonitor(this) }

    private val viewModel: MediaPickerViewModel by viewModels()
    private val castViewModel: CastViewModel by viewModels()

    @Inject
    lateinit var navigator: AppNavigator
    private var loadingDialog: LoadingDialog? = null

    private val mediaType: MediaType by lazy {
        MediaType.valueOf(intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: MediaType.IMAGE.name)
    }

    private val mediaAdapter = MediaItemAdapter { item ->
        val isConnected = castViewModel.uiState.value.let { state ->
            state is CastUiState.Connected || (state is CastUiState.HasDevices && state.devices.any { it.isConnected })
        }
        hideDevicesPanel()
        viewModel.onMediaItemClicked(item, isConnected)
    }

    private val deviceAdapter = DeviceAdapter(
        onDeviceClick = { device ->
            if (device.isConnected) {
                castViewModel.connectDevice(device.id)
                castViewModel.disconnect()
                shouldDisconnectAfterConnect = true
            } else {
                castViewModel.connectDevice(device.id)
            }
        },
        onDeviceLongClick = { device -> castViewModel.requestPinForDevice(device.id) },
    )

    private var pinDialog: PinDialog? = null
    private var shouldDisconnectAfterConnect = false
    private var currentItemDecoration: androidx.recyclerview.widget.RecyclerView.ItemDecoration? = null

    override fun getLayoutActivity(): Int = R.layout.cast_activity_media_picker

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) =
        CastActivityMediaPickerBinding.inflate(inflater)

    private fun stateFlow() = viewModel.uiState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectState(viewModel.events, ::handleEvent)
        collectState(castViewModel.uiState, ::renderCastState)
        collectState(viewModel.castingItemId) { id -> mediaAdapter.castingItemId = id }
        collectState(viewModel.isCasting) { isCasting ->

                if (isCasting || viewModel.uiState.value is MediaUiState.Loading) showLoadingDialog() else loadingDialog?.dismiss()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (mBinding.flDevices.visibility == View.VISIBLE) {
                hideDevicesPanel()
            } else {
                finish()
            }
        }
    }

    private fun setupViews() {
        loadBanner()
        Glide.with(this).load(R.drawable.cast_img_background).centerCrop().into(mBinding.imgBackground)
        setupTopBar()
        setupDevicesPanel()
        setupMediaTypeSelector()
        setupRecyclerView()
        viewModel.loadMedia(mediaType)
    }

    fun loadBanner(){
        AdsManager.loadBanner(this, AdRemoteConfig.banner_all, mBinding.frAdsBanner, false)
    }

    private fun setupTopBar() {
        mBinding.topBar.imgBack.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.text = getString(R.string.cast_cast_title)
        mBinding.topBar.imgSwap.visibility = View.GONE
        mBinding.topBar.imgCast.visibility = View.VISIBLE
        mBinding.topBar.lnActionHome.visibility = View.GONE
        mBinding.topBar.tvTitleHome.visibility = View.GONE
    }

    private fun setupDevicesPanel() {
        mBinding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupMediaTypeSelector() {
        mBinding.mediaTypeSelector.setSelectedType(mediaType)
        mBinding.mediaTypeSelector.setOnTypeSelectedListener { type ->
            hideDevicesPanel()
            mBinding.recyclerView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    mediaAdapter.submitList(emptyList())
                    applyGridColumns(type)
                    viewModel.loadMedia(type)
                    mBinding.recyclerView.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }

    private fun setupRecyclerView() {
        mBinding.recyclerView.itemAnimator = null
        applyGridColumns(mediaType)
        mBinding.recyclerView.adapter = mediaAdapter
    }

    private fun applyGridColumns(type: MediaType) {
        val spanCount = when (type) {
            MediaType.IMAGE -> 3
            MediaType.VIDEO -> 2
            MediaType.AUDIO -> 3
        }
        
        currentItemDecoration?.let { mBinding.recyclerView.removeItemDecoration(it) }
        
        val spacing = (8 * resources.displayMetrics.density).toInt()
        val newDecoration = GridSpacingItemDecoration(spanCount, spacing, includeEdge = true)
        mBinding.recyclerView.addItemDecoration(newDecoration)
        currentItemDecoration = newDecoration
        
        val lm = mBinding.recyclerView.layoutManager
        if (lm is GridLayoutManager) {
            lm.spanCount = spanCount
        } else {
            mBinding.recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        }
    }

    private fun setupClicks() {
        mBinding.topBar.imgBack.animateClick {
            AdsManager.loadAndShowInterstitial(this@MediaPickerActivity, AdRemoteConfig.inter_back){
                onBackPressed()
            }
        }
        mBinding.topBar.imgCast.setOnClickListener {
            if (hasAvailableDevices()) toggleDevicesPanel() else navigator.openCastScreen(this)
        }
        mBinding.viewOverlay.setOnClickListener {
            hideDevicesPanel()
        }
    }

    private fun hasAvailableDevices(): Boolean = when (val state = castViewModel.uiState.value) {
        is CastUiState.HasDevices -> state.devices.isNotEmpty()
        is CastUiState.Connected -> true
        else -> false
    }

    private fun toggleDevicesPanel() {
        if (mBinding.flDevices.isVisible) {
            hideDevicesPanel()
        } else {
            showDevicesPanel()
        }
    }

    private fun showDevicesPanel() {
        val panel = mBinding.flDevices

        mBinding.viewOverlay.visibility = View.VISIBLE
        mBinding.viewOverlay.alpha = 0f
        mBinding.viewOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        panel.alpha = 0f
        panel.visibility = View.VISIBLE

        panel.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                panel.viewTreeObserver.removeOnPreDrawListener(this)
                panel.translationY = -panel.height.toFloat()
                panel.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .start()
                return true
            }
        })
    }

    private fun hideDevicesPanel() {
        val panel = mBinding.flDevices

        mBinding.viewOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                mBinding.viewOverlay.visibility = View.GONE
            }
            .start()

        panel.animate()
            .translationY(-panel.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                panel.visibility = View.GONE
                panel.translationY = 0f
                panel.alpha = 1f
            }
            .start()
    }

    private fun renderState(state: MediaUiState) {
            if (state is MediaUiState.Loading || viewModel.isCasting.value) showLoadingDialog() else loadingDialog?.dismiss()
        mBinding.recyclerView.visibility =
            if (state is MediaUiState.Content) View.VISIBLE else View.GONE
        mBinding.tvEmpty.visibility = if (state is MediaUiState.Empty) View.VISIBLE else View.GONE
        if (state is MediaUiState.Content) {
            mediaAdapter.submitList(state.items) {
                mBinding.recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun renderCastState(state: CastUiState) {
        val isConnected = when (state) {
            is CastUiState.HasDevices -> state.devices.any { it.isConnected }
            is CastUiState.Connected -> true
            else -> false
        }
        mBinding.topBar.imgCast.setImageResource(
            if (isConnected) R.drawable.cast_ic_cast else R.drawable.cast_ic_cast_disnable
        )
        if (state !is CastUiState.PinRequired) dismissPinDialog()
        when (state) {

            CastUiState.Loading -> deviceAdapter.submitList(emptyList())
            is CastUiState.HasDevices -> {
                deviceAdapter.submitList(state.devices)
                deviceAdapter.connectingId = state.connectingId
            }
            is CastUiState.Connected -> {
                if (shouldDisconnectAfterConnect) {
                    Log.e("TAG", "shouldDisconnectAfterConnect: $shouldDisconnectAfterConnect")
                    shouldDisconnectAfterConnect = false
                    castViewModel.disconnect()
                    return
                }
                
                deviceAdapter.submitList(state.devices)
                deviceAdapter.connectingId = null
            }
            CastUiState.Empty, CastUiState.LostConnection -> {
                deviceAdapter.submitList(emptyList())
                deviceAdapter.connectingId = null
            }
            is CastUiState.PinRequired -> showPinDialog(state.device)
        }
    }

    private fun showPinDialog(device: com.anrstudio.template.domain.model.CastDevice) {
        if (pinDialog?.isShowing == true) return
        pinDialog = PinDialog(
            context = this,
            deviceName = device.name,
            onConfirm = { pin -> castViewModel.submitPin(pin) },
            onCancel = { castViewModel.cancelPin() },
        ).also { it.show() }
    }

    private fun dismissPinDialog() {
        pinDialog?.dismiss()
        pinDialog = null
    }

    private fun handleEvent(event: MediaEvent) {
        when (event) {
            MediaEvent.NavigateToCast -> navigator.openCastScreen(this, fromMediaPicker = true)
            is MediaEvent.ShowError -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        loadingDialog = LoadingDialog(
            this
        ).also { it.show() }
    }
    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
    }
}
