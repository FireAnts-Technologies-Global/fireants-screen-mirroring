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
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.anrstudio.template.R
import com.anrstudio.template.navigation.AppNavigator
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.StatusBarConfig
import com.anrstudio.template.databinding.CastActivityMediaPickerBinding
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.domain.model.MediaTab
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

    override val statusBarConfig = StatusBarConfig(
        barColor = android.graphics.Color.rgb(234, 242, 255),
        lightIcons = true,
    )

    private val networkMonitor by lazy { com.anrstudio.template.utils.CastNetworkMonitor(this) }

    private val viewModel: MediaPickerViewModel by viewModels()
    private val castViewModel: CastViewModel by viewModels()

    @Inject
    lateinit var navigator: AppNavigator
    private var loadingDialog: LoadingDialog? = null

    private val mediaType: MediaType by lazy {
        MediaType.valueOf(intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: MediaType.IMAGE.name)
    }

    private var previewItem: com.anrstudio.template.domain.model.MediaItem? = null
    private var currentTab: MediaTab = MediaTab.PHOTOS
    private val mediaAdapter = MediaItemAdapter { item ->
        if (item.type == MediaType.IMAGE) showPhotoPreview(item) else castMediaItem(item)
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
        mediaPickerStyle = true,
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
            if (mBinding.previewContainer.isVisible) {
                hidePhotoPreview()
            } else if (mBinding.flDevices.visibility == View.VISIBLE) {
                hideDevicesPanel()
            } else {
                finish()
            }
        }
    }

    private fun setupViews() {
        currentTab = mediaType.toMediaTab()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = getColor(R.color.cast_cast_background_start)
        loadBanner()
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
        mBinding.topBar.root.setBackgroundColor(Color.TRANSPARENT)
        mBinding.topBar.root.setPadding(dp(16), dp(8), dp(16), dp(8))
        mBinding.topBar.tvTitle.text = getString(
            if (mediaType == MediaType.VIDEO) R.string.cast_media_title_videos else R.string.cast_media_title_photos
        )
        mBinding.topBar.tvTitle.setTextColor(getColor(R.color.cast_cast_primary_text))
        mBinding.topBar.tvTitle.typeface = androidx.core.content.res.ResourcesCompat.getFont(
            this,
            R.font.inter_bold,
        )
        mBinding.topBar.tvTitle.textSize = 22f
        mBinding.topBar.tvTitle.setPadding(0, 0, 0, 0)
        (mBinding.topBar.tvTitle.layoutParams as android.widget.RelativeLayout.LayoutParams).apply {
            marginStart = dp(52)
            marginEnd = dp(52)
        }.also { mBinding.topBar.tvTitle.layoutParams = it }
        mBinding.topBar.imgBack.apply {
            layoutParams = android.widget.RelativeLayout.LayoutParams(dp(44), dp(44)).apply {
                addRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
            }
            setImageResource(R.drawable.cast_ic_figma_back)
            background = getDrawable(R.drawable.cast_bg_media_back)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        mBinding.topBar.imgCast.apply {
            layoutParams = android.widget.RelativeLayout.LayoutParams(dp(44), dp(44)).apply {
                addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
            }
            background = getDrawable(R.drawable.cast_bg_media_cast_button)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setImageResource(R.drawable.cast_ic_home_cast_white)
        }
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
        mBinding.mediaTypeSelector.setSelectedTab(mediaType.toMediaTab())
        mBinding.mediaTypeSelector.setOnTabSelectedListener { tab ->
            currentTab = tab
            resetTopBarTitle()
            hideDevicesPanel()
            mBinding.recyclerView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    mediaAdapter.submitList(emptyList())
                    applyGridColumns(tab)
                    viewModel.loadMedia(tab)
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
        applyGridColumns(mediaType.toMediaTab())
        mBinding.recyclerView.adapter = mediaAdapter
    }

    private fun applyGridColumns(tab: MediaTab) {
        val spanCount = when (tab) {
            MediaTab.VIDEOS -> 2
            else -> 3
        }
        
        currentItemDecoration?.let { mBinding.recyclerView.removeItemDecoration(it) }
        
        val spacing = dp(12)
        val newDecoration = GridSpacingItemDecoration(spanCount, spacing, includeEdge = false)
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
        mBinding.btnCastPhoto.setOnClickListener {
            previewItem?.let(::castMediaItem)
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
        mBinding.topBar.tvTitle.text = getString(R.string.cast_cast_title)
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
        if (!mBinding.previewContainer.isVisible) resetTopBarTitle()
    }

    private fun showPhotoPreview(item: com.anrstudio.template.domain.model.MediaItem) {
        previewItem = item
        hideDevicesPanel()
        mBinding.recyclerView.visibility = View.GONE
        mBinding.tvEmpty.visibility = View.GONE
        mBinding.mediaTypeSelector.visibility = View.GONE
        mBinding.previewContainer.visibility = View.VISIBLE
        mBinding.topBar.imgCast.visibility = View.GONE
        mBinding.topBar.tvTitle.text = getString(R.string.cast_media_cast_photo)
        mBinding.topBar.tvTitle.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
        Glide.with(this).load(Uri.parse(item.uri)).centerCrop().into(mBinding.imgPhotoPreview)
    }

    private fun hidePhotoPreview() {
        previewItem = null
        Glide.with(this).clear(mBinding.imgPhotoPreview)
        mBinding.previewContainer.visibility = View.GONE
        mBinding.topBar.imgCast.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.gravity = android.view.Gravity.CENTER
        resetTopBarTitle()
        val state = viewModel.uiState.value
        mBinding.recyclerView.visibility = if (state is MediaUiState.Content) View.VISIBLE else View.GONE
        mBinding.tvEmpty.visibility = if (state is MediaUiState.Empty) View.VISIBLE else View.GONE
        mBinding.mediaTypeSelector.visibility = View.VISIBLE
    }

    private fun castMediaItem(item: com.anrstudio.template.domain.model.MediaItem) {
        val isConnected = castViewModel.uiState.value.let { state ->
            state is CastUiState.Connected || (state is CastUiState.HasDevices && state.devices.any { it.isConnected })
        }
        hidePhotoPreview()
        hideDevicesPanel()
        viewModel.onMediaItemClicked(item, isConnected)
    }

    private fun resetTopBarTitle() {
        mBinding.topBar.tvTitle.text = getString(
            if (currentTab == MediaTab.VIDEOS) R.string.cast_media_title_videos else R.string.cast_media_title_photos
        )
    }

    private fun MediaType.toMediaTab(): MediaTab = when (this) {
        MediaType.IMAGE -> MediaTab.PHOTOS
        MediaType.VIDEO -> MediaTab.VIDEOS
        MediaType.AUDIO -> MediaTab.AUDIO
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
        mBinding.topBar.imgCast.setImageResource(R.drawable.cast_ic_home_cast_white)
        if (state !is CastUiState.PinRequired) dismissPinDialog()
        when (state) {

            CastUiState.Loading -> {
                deviceAdapter.selectedId = null
                deviceAdapter.submitList(emptyList())
            }
            is CastUiState.HasDevices -> {
                deviceAdapter.submitList(state.devices)
                deviceAdapter.connectingId = state.connectingId
                deviceAdapter.selectedId = state.selectedId
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
                deviceAdapter.selectedId = state.connectedDevice.id
            }
            CastUiState.Empty, CastUiState.LostConnection -> {
                deviceAdapter.submitList(emptyList())
                deviceAdapter.connectingId = null
                deviceAdapter.selectedId = null
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
