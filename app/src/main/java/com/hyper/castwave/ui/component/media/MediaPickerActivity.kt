package com.hyper.castwave.ui.component.media

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.banner_cast_image_collage
import com.hyper.castwave.ads.banner_cast_video_collage
import com.hyper.castwave.databinding.CastActivityMediaPickerBinding
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaType
import com.hyper.castwave.navigation.AppNavigator
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.StatusBarConfig
import com.hyper.castwave.ui.component.cast.CastUiState
import com.hyper.castwave.ui.component.cast.CastViewModel
import com.hyper.castwave.ui.component.cast.DeviceAdapter
import com.hyper.castwave.ui.component.cast.PinDialog
import com.hyper.castwave.ui.component.dialog.LoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.intuit.sdp.R as SdpR

@AndroidEntryPoint
class MediaPickerActivity : BaseActivity<CastActivityMediaPickerBinding>() {

    override val statusBarConfig = StatusBarConfig(
        barColor = Color.rgb(234, 242, 255),
        lightIcons = true,
    )

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    private val viewModel: MediaPickerViewModel by viewModels()
    private val castViewModel: CastViewModel by viewModels()

    @Inject
    lateinit var navigator: AppNavigator
    private var loadingDialog: LoadingDialog? = null

    private val mediaType: MediaType by lazy {
        MediaType.valueOf(intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: MediaType.IMAGE.name)
    }

    @Suppress("DEPRECATION")
    private val pickedUri: Uri by lazy {
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PICKED_URI, Uri::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_PICKED_URI)
        }
        requireNotNull(uri) { "MediaPickerActivity requires EXTRA_PICKED_URI" }
    }

    private var previewItem: MediaItem? = null

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

    override fun getLayoutActivity(): Int = R.layout.cast_activity_media_picker

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        setupViews()
        setupClicks()
    }

    override fun inflateBinding(inflater: LayoutInflater) =
        CastActivityMediaPickerBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectState(viewModel.events, ::handleEvent)
        collectState(castViewModel.uiState, ::renderCastState)
        collectState(viewModel.isCasting) { isCasting ->
            if (isCasting) showLoadingDialog() else loadingDialog?.dismiss()
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
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = getColor(R.color.cast_cast_background_start)
        Log.d("MediaPicker", "setupViews: mediaType=$mediaType pickedUri=$pickedUri")
        loadBanner()
        setupTopBar()
        setupDevicesPanel()
        mBinding.btnCastPhoto.text = getString(
            if (mediaType == MediaType.VIDEO) R.string.cast_media_cast_video
            else R.string.cast_media_cast_photo
        )
        showPhotoPreview(buildPickedMediaItem(pickedUri))
    }

    private fun buildPickedMediaItem(uri: Uri): MediaItem {
        val uriString = uri.toString()
        return MediaItem(
            id = uriString,
            name = uri.lastPathSegment ?: "",
            uri = uriString,
            thumbnailUri = uriString,
            type = mediaType,
            duration = null,
            dateAdded = 0L,
            isFavorite = false,
        )
    }

    private fun loadBanner() {
        val config = if (mediaType == MediaType.VIDEO)
            AdRemoteConfig.banner_cast_video_collage
        else
            AdRemoteConfig.banner_cast_image_collage
        AdsManager.loadBannerCollapsible(this, config, mBinding.frAdsBanner)
    }

    private fun setupTopBar() {
        mBinding.topBar.setVariant(com.hyper.castwave.ui.component.custom.FigmaTopBarView.Variant.CENTERED_ACTIONS)
        mBinding.topBar.setBackIconSize(resources.getDimensionPixelSize(SdpR.dimen._14sdp))
        val titleRes = if (mediaType == MediaType.VIDEO)
            R.string.cast_media_cast_video
        else
            R.string.cast_media_cast_photo
        mBinding.topBar.setTitle(getString(titleRes))
        mBinding.topBar.setOnBackClickListener(animated = true) {
            AdsManager.loadAndShowInterBack(this@MediaPickerActivity) {
                onBackPressed()
            }
        }
        mBinding.topBar.setEndAction(
            R.drawable.cast_ic_home_cast_white,
            getString(R.string.cast_cast_title),
            {
                if (hasAvailableDevices()) toggleDevicesPanel() else navigator.openCastScreen(this)
            },
            R.drawable.cast_bg_media_cast_button,
        )
    }

    private fun setupDevicesPanel() {
        mBinding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupClicks() {
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

    private fun showPhotoPreview(item: MediaItem) {
        previewItem = item
        mBinding.previewContainer.visibility = View.VISIBLE
        Glide.with(this).load(Uri.parse(item.uri)).centerCrop().into(mBinding.imgPhotoPreview)
    }

    private fun castMediaItem(item: MediaItem) {
        val isConnected = castViewModel.uiState.value.let { state ->
            state is CastUiState.Connected || (state is CastUiState.HasDevices && state.devices.any { it.isConnected })
        }
        hideDevicesPanel()
        lifecycleScope.launch {
            showLoadingDialog()
            val cachedFile = withContext(Dispatchers.IO) {
                copyPickedUriToCache(Uri.parse(item.uri))
            }
            if (cachedFile == null) {
                loadingDialog?.dismiss()
                Toast.makeText(
                    this@MediaPickerActivity,
                    getString(R.string.cast_media_prepare_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            val castable = item.copy(uri = Uri.fromFile(cachedFile).toString())
            viewModel.onMediaItemClicked(castable, isConnected)
        }
    }

    private fun copyPickedUriToCache(uri: Uri): java.io.File? {
        return try {
            val extension = when (mediaType) {
                MediaType.IMAGE -> ".jpg"
                MediaType.VIDEO -> ".mp4"
                MediaType.AUDIO -> ".mp3"
            }
            val dir = java.io.File(cacheDir, "cast_picked").apply { mkdirs() }
            val outFile = java.io.File(dir, "picked_${System.currentTimeMillis()}$extension")
            contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            outFile
        } catch (e: Exception) {
            Log.e("MediaPicker", "copyPickedUriToCache failed", e)
            null
        }
    }

    private fun renderCastState(state: CastUiState) {
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

    private fun showPinDialog(device: com.hyper.castwave.domain.model.CastDevice) {
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
        loadingDialog = LoadingDialog(this).also { it.show() }
    }

    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_PICKED_URI = "extra_picked_uri"
    }
}
