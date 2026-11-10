package com.hyper.castwave.ui.component.webbrowser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.databinding.CastActivityWebBrowserBinding
import com.hyper.castwave.domain.model.CastDevice
import com.hyper.castwave.navigation.AppNavigator
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.ext.animateClick
import com.hyper.castwave.ui.component.cast.CastUiState
import com.hyper.castwave.ui.component.cast.CastViewModel
import com.hyper.castwave.ui.component.cast.DeviceAdapter
import com.hyper.castwave.ui.component.cast.PinDialog
import com.hyper.castwave.ui.component.dialog.LoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebBrowserActivity : BaseActivity<CastActivityWebBrowserBinding>() {

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val PERIODIC_DETECTION_INTERVAL = 3000L

        fun newIntent(context: Context, url: String): Intent =
            Intent(context, WebBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    private val viewModel: WebBrowserViewModel by viewModels()
    private val castViewModel: CastViewModel by viewModels()

    @Inject
    lateinit var navigator: AppNavigator

    private val initialUrl: String by lazy {
        intent.getStringExtra(EXTRA_URL) ?: error("Missing EXTRA_URL")
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
    private var loadingDialog: LoadingDialog? = null
    private var shouldDisconnectAfterConnect = false
    private var pendingVideoDetection: Runnable? = null
    private var periodicDetectionRunnable: Runnable? = null
    private var lastDetectedUrl: String? = null

    override fun getLayoutActivity(): Int = R.layout.cast_activity_web_browser

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) =
        CastActivityWebBrowserBinding.inflate(inflater)

    private fun stateFlow() = viewModel.uiState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectState(castViewModel.uiState, ::renderCastState)
        collectState(viewModel.events, ::handleEvent)
        collectState(viewModel.isCasting) { isCasting ->
            mBinding.progressBar.visibility =
                if (isCasting || viewModel.uiState.value.isLoading) View.VISIBLE else View.GONE
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mBinding.flDevices.isVisible) {
                    hideDevicesPanel()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        stopPeriodicDetection()
        super.onDestroy()
    }

    private fun setupViews() {
        setupTopBar()
        setupDevicesPanel()
        setupWebView()
    }

    private fun setupClicks() {
        mBinding.viewOverlay.setOnClickListener {
            hideDevicesPanel()
        }
        mBinding.topBar.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        mBinding.topBar.imgCast.animateClick {
            if (hasAvailableDevices()) toggleDevicesPanel() else navigator.openCastScreen(this,fromMediaPicker = true)
        }
        mBinding.imgBrowserBack.animateClick {
            hideDevicesPanel()
            if (mBinding.webView.canGoBack()) mBinding.webView.goBack()
        }
        mBinding.imgBrowserForward.animateClick {
            hideDevicesPanel()
            if (mBinding.webView.canGoForward()) mBinding.webView.goForward()
        }
        mBinding.imgMediaPick.animateClick {
            hideDevicesPanel()
            val storedVideos = viewModel.uiState.value.videos
            if (storedVideos.isEmpty()) {
                return@animateClick
            }

            val isConnected = castViewModel.uiState.value.let { state ->
                state is CastUiState.Connected || (state is CastUiState.HasDevices && state.devices.any { it.isConnected })
            }

            VideoListBottomSheet { selectedVideo ->
                hideDevicesPanel()
                viewModel.castVideo(selectedVideo, isConnected)
                showLoadingDialog()
            }.show(supportFragmentManager, "VideoListBottomSheet")
        }
        mBinding.imgBrowserHome.animateClick {
            hideDevicesPanel()
            mBinding.webView.loadUrl(initialUrl)
        }
        mBinding.imgBrowserReload.animateClick {
            hideDevicesPanel()
            mBinding.webView.reload()
        }
    }

    private fun renderState(state: WebBrowserUiState) {
        mBinding.progressBar.visibility =
            if (state.isLoading || viewModel.isCasting.value) View.VISIBLE else View.GONE
        if (state.isLoading) mBinding.progressBar.progress = state.progress
        mBinding.topBar.tvTitle.text = state.title.ifEmpty { initialUrl }
        mBinding.imgBrowserBack.alpha = if (state.canGoBack) 1f else 0.3f
        mBinding.imgBrowserForward.alpha = if (state.canGoForward) 1f else 0.3f
        mBinding.imgMediaPick.apply {
            isEnabled = true
            alpha = 1f
            setImageResource(if (state.hasVideos) R.drawable.cast_ic_yt_list else R.drawable.cast_ic_yt_list_empty)
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
            CastUiState.Loading -> {
                deviceAdapter.submitList(emptyList())
            }
            is CastUiState.HasDevices -> {
                deviceAdapter.submitList(state.devices)
                deviceAdapter.connectingId = state.connectingId
            }
            is CastUiState.Connected -> {
                if (shouldDisconnectAfterConnect) {
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
            is CastUiState.PinRequired -> {
                showPinDialog(state.device)
            }
        }
    }

    private fun hasAvailableDevices(): Boolean = when (val state = castViewModel.uiState.value) {
        is CastUiState.HasDevices -> state.devices.isNotEmpty()
        is CastUiState.Connected -> true
        else -> false
    }

    private fun setupTopBar() {
        mBinding.topBar.imgBack.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.visibility = View.VISIBLE
        mBinding.topBar.imgCast.visibility = View.VISIBLE
        mBinding.topBar.imgSwap.visibility = View.GONE
        mBinding.topBar.lnActionHome.visibility = View.GONE
        mBinding.topBar.tvTitleHome.visibility = View.GONE
    }

    private fun setupDevicesPanel() {
        mBinding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupWebView() {
        mBinding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        mBinding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                stopPeriodicDetection()
                pendingVideoDetection?.let { view.removeCallbacks(it) }
                pendingVideoDetection = null
                lastDetectedUrl = null
                viewModel.clearVideoDetection()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                
                if (request.isForMainFrame) {
                    if (url.startsWith("snssdk") ||
                        (url.contains("tiktok.com") && url.contains("aweme/detail"))) {
                        return true
                    }
                }
                
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!url.startsWith("snssdk") &&
                    !(url.contains("tiktok.com") && url.contains("aweme/detail"))) {
                    
                    viewModel.updateTitle(view.title ?: url)
                    viewModel.updateUrl(url)
                    viewModel.updateNavState(view.canGoBack(), view.canGoForward())
                    
                    lastDetectedUrl = url
                    
                    val isYouTube = url.contains("youtube.com") || url.contains("youtu.be")

                    if (isYouTube) {
                        viewModel.detectVideos(url)
                        startPeriodicDetection(view)
                    } else if ((url.contains("tiktok.com") && (url.contains("/explore") || url.contains("/foryou"))) ||
                        (url.contains("vimeo.com") && url.contains("/channels/"))) {
                        
                        val runnable = Runnable {
                            viewModel.detectVideosFromWebView(view)
                        }
                        pendingVideoDetection = runnable
                        view.postDelayed(runnable, 2000)
                        
                        startPeriodicDetection(view)
                    } else {
                        viewModel.detectVideos(url)
                        startPeriodicDetection(view)
                    }
                }
            }
        }

        mBinding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                viewModel.updateProgress(newProgress)
            }
        }

        mBinding.webView.loadUrl(initialUrl)
    }

    private fun toggleDevicesPanel() {
        if (mBinding.flDevices.visibility == View.VISIBLE) {
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


    private fun showPinDialog(device: CastDevice) {
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

    private fun handleEvent(event: WebBrowserEvent) {
        when (event) {

            WebBrowserEvent.NavigateToCast -> {
                lifecycleScope.launch {
                    delay(2000)
                    loadingDialog?.dismiss()
                    delay(500)
                    navigator.openCastScreen(this@WebBrowserActivity, fromMediaPicker = true)
                }
            }

            is WebBrowserEvent.ShowError -> {
                lifecycleScope.launch {
                    delay(2000)
                    loadingDialog?.dismiss()

                    delay(500)
                    Toast.makeText(
                        this@WebBrowserActivity,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            WebBrowserEvent.NavigateToVideoControl -> {
                lifecycleScope.launch {
                    delay(2000)
                    loadingDialog?.dismiss()
                    delay(500)


                    AdsManager.loadAndShowInterHome(this@WebBrowserActivity) {
                        navigator.openVideoControl(this@WebBrowserActivity)
                    }
                }
            }
        }
    }

    private fun startPeriodicDetection(webView: WebView) {
        stopPeriodicDetection()
        periodicDetectionRunnable = object : Runnable {
            override fun run() {
                val currentUrl = webView.url
                if (currentUrl != null && currentUrl != lastDetectedUrl) {
                    lastDetectedUrl = currentUrl
                    if (currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be")) {
                        viewModel.detectVideos(currentUrl)
                    } else {
                        viewModel.detectVideosFromWebView(webView)
                    }
                } else {
                    if (currentUrl != null && (currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be"))) {
                    } else {
                        viewModel.detectVideosFromWebView(webView)
                    }
                }
                
                webView.postDelayed(this, PERIODIC_DETECTION_INTERVAL)
            }
        }
        
        webView.postDelayed(periodicDetectionRunnable!!, PERIODIC_DETECTION_INTERVAL)
    }

    private fun stopPeriodicDetection() {
        periodicDetectionRunnable?.let {
            mBinding.webView.removeCallbacks(it)
            periodicDetectionRunnable = null
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        loadingDialog = LoadingDialog(
            this
        ).also { it.show() }
    }
}
