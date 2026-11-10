package com.hyper.castwave.ui.component.iptv.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.banner_player
import com.hyper.castwave.app.GlobalApp.Companion.isInPIP
import com.hyper.castwave.data.iptv.model.DataState
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.databinding.CastActivityPlayListBinding
import com.hyper.castwave.ui.bases.BannerConfig
import com.hyper.castwave.ui.bases.StatusBarConfig
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.bases.ext.goneView
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.ui.bases.ext.isVisible
import com.hyper.castwave.ui.bases.ext.observeFlow
import com.hyper.castwave.ui.bases.ext.parcelable
import com.hyper.castwave.ui.bases.ext.showToastById
import com.hyper.castwave.ui.bases.ext.visibleView
import com.hyper.castwave.ui.component.iptv.IptvAppConstants
import com.hyper.castwave.ui.component.iptv.IptvAppConstants.EXTRA_SHOW_ADS_WHEN_BACK
import com.hyper.castwave.ui.component.iptv.IptvBaseActivity
import com.hyper.castwave.ui.component.iptv.dialog.VideoEndDialog
import com.hyper.castwave.ui.component.iptv.dialog.VideoEndDialog.Companion.TAG_VIDEO_END_DIALOG
import com.hyper.castwave.ui.component.iptv.dialog.VideoErrorDialog
import com.hyper.castwave.ui.component.iptv.dialog.VideoErrorDialog.Companion.TAG_VIDEO_ERROR_DIALOG
import com.hyper.castwave.ui.component.iptv.dialog.VideoTimerDialog
import com.hyper.castwave.ui.component.iptv.dialog.VideoTimerDialog.Companion.TAG_VIDEO_TIMER_DIALOG
import com.hyper.castwave.ui.component.iptv.player.adapter.TVChannelAdapter
import com.hyper.castwave.ui.component.iptv.player.viewmodel.ChannelEvent
import com.hyper.castwave.ui.component.iptv.player.viewmodel.ChannelViewModel
import com.hyper.castwave.ui.component.iptv.util.CalendarUtil.TIME_ONE_SECOND
import com.hyper.castwave.ui.component.iptv.util.CastHelper
import com.hyper.castwave.ui.component.iptv.viewmodel.IptvHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar

@AndroidEntryPoint
@UnstableApi
class PlayListActivity : IptvBaseActivity<CastActivityPlayListBinding>(),
    VideoEndDialog.VideoEndDialogListener, VideoErrorDialog.VideoErrorDialogListener,
    VideoTimerDialog.VideoTimerDialogListener {

    override fun getLayoutActivity() = R.layout.cast_activity_play_list

    override val bannerConfig: BannerConfig
        get() = BannerConfig(AdRemoteConfig.banner_player, isCollapse = false)

    override val statusBarConfig = StatusBarConfig(
        barColor = Color.rgb(234, 242, 255),
        lightIcons = true,
    )

    override fun requestWindow() {
        super.requestWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
    }

    companion object {
        private const val TAG = "PlayListActivity"
        const val EXTRA_TV_CHANEL = "extra_tv_chanel"
        const val EXTRA_GROUP_TYPE = "extra_group_type"
        const val EXTRA_FILTER_TYPE = "extra_filter_type"

        private const val TAG_SEARCH_EDITTEXT = "tag_search_edittext"

        private const val TIME_SHOW_CONTROLLER = 5000L

        private const val DEFAULT_REFERRER = "http://example.com/"

        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    private val channelViewModel by viewModels<ChannelViewModel>()

    private val homeViewModel by viewModels<IptvHomeViewModel>()

    private lateinit var channelAdapter: TVChannelAdapter

    private var tvChanel: TVChannel? = null
    private var groupType: String = IptvAppConstants.GROUP_TYPE_CATEGORY
    private var filterType: String = ""

    private var player: ExoPlayer? = null

    private var btnClose: ImageButton? = null
    private var btnCloseFull: ImageButton? = null

    private var btnFavorite: ImageButton? = null
    private var btnFavoriteFull: ImageButton? = null

    private var tvName: TextView? = null
    private var tvNameFull: TextView? = null

    private var tvIndex: TextView? = null
    private var tvIndexFull: TextView? = null

    private var btnNext: ImageButton? = null
    private var btnNextFull: ImageButton? = null

    private var btnToggle: ImageButton? = null
    private var btnToggleFull: ImageButton? = null

    private var btnPrev: ImageButton? = null
    private var btnPrevFull: ImageButton? = null

    private var btnPip: LinearLayout? = null
    private var btnPipFull: LinearLayout? = null

    private var btnTimer: LinearLayout? = null
    private var btnTimerFull: LinearLayout? = null

    private var btnLock: LinearLayout? = null
    private var btnLockFull: LinearLayout? = null

    private var btnFullScreen: LinearLayout? = null
    private var btnFullScreenFull: LinearLayout? = null

    private var btnCast: LinearLayout? = null
    private var btnCastFull: LinearLayout? = null

    private var btnListFull: ImageButton? = null

    private var isNeedCheckTime = false

    private var handler: Handler? = null

    private var runnable = object : Runnable {

        override fun run() {
            val currentTime = System.currentTimeMillis()
            val timeSet = iptvSharedPref.timeNeedPauseVideo
            if (timeSet > currentTime) {
                isNeedCheckTime = true
                handler?.postDelayed(this, TIME_ONE_SECOND)
            } else {
                if (isNeedCheckTime) {
                    isNeedCheckTime = false
                    try {
                        onActivityBackPressed()
                    } catch (_: Exception) {}
                }
                handler?.removeCallbacksAndMessages(null)
            }
        }
    }




    private var handlerController: Handler? = null

    private var runnableController = Runnable {
        try {
            if (!isFullScreen) {
                btnCast?.goneView()
                mBinding.playerView.findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.goneView()
                updateStateVideoController(false)
                mBinding.root.findViewById<ImageButton>(R.id.btnPlayOverlay)?.visibleView()
                mBinding.btnFullScreenPersistent.visibleView()
            }
        } catch (_: Exception) {}
    }

    private var showLockJob: Job? = null

    private var isFullScreen = false
    private var playerRootPaddingTop = 0

    private var castHelper: CastHelper? = null
    private var mediaRouteButton: MediaRouteButton? = null
    private var mediaRouteButtonFull: MediaRouteButton? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val newTVChanel = intent.parcelable<TVChannel>(EXTRA_TV_CHANEL)
        val newGroupType = intent?.getStringExtra(EXTRA_GROUP_TYPE) ?: IptvAppConstants.GROUP_TYPE_CATEGORY
        val newFilterType = intent?.getStringExtra(EXTRA_FILTER_TYPE) ?: ""
        if (newTVChanel != null) {
            if (newTVChanel.url != tvChanel?.url) {
                isInPIP = false
                tvChanel = newTVChanel
                groupType = newGroupType
                filterType = newFilterType
                initViewIdForController()
                initPlayerForUrl(tvChanel!!)
                channelAdapter.updateSelectedItem(tvChanel!!)
                channelViewModel.onEvent(ChannelEvent.GetAllChanelForGroup(this, tvChanel!!, groupType, filterType))
            }
        }
    }

    override fun initViews() {
        super.initViews()
        isHasShowAddWhenBack = intent.getBooleanExtra(EXTRA_SHOW_ADS_WHEN_BACK, false)

        handler = Handler(Looper.getMainLooper())
        handlerController = Handler(Looper.getMainLooper())
        handler?.postDelayed(runnable, TIME_ONE_SECOND)

        tvChanel = intent.parcelable(EXTRA_TV_CHANEL)
        groupType = intent.getStringExtra(EXTRA_GROUP_TYPE) ?: IptvAppConstants.GROUP_TYPE_CATEGORY
        filterType = intent.getStringExtra(EXTRA_FILTER_TYPE) ?: ""
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())
        windowInsetsController.isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.cast_cast_background_start)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (tvChanel == null) {
            Log.d(TAG, "onStart Channel Null")
            showErrorDialog()
        } else {
            Log.d(TAG, "onStart Channel Ready")
            initViewIdForController()
            initializeCast()
            initPlayerForUrl(tvChanel!!)
            initAdapter()
            channelViewModel.onEvent(ChannelEvent.GetAllChanelForGroup(this, tvChanel!!, groupType, filterType))
        }
    }

    override fun observeData() {
        super.observeData()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                observeFlow(channelViewModel.dataTVChannelState) {
                    updateDataChannelState(it)
                }
            }
        }
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.layoutLockPlayerView.click {
            showWithAutoHide(mBinding.layoutLockContent)
        }

        mBinding.layoutLockPlayerView.setOnLongClickListener {
            showLockScreen(isFullScreen = false, isLock = false)
            return@setOnLongClickListener true
        }

        mBinding.layoutLockPlayerViewFull.click {
            showWithAutoHide(mBinding.layoutLockContentFull)
            if (mBinding.layoutChanelFull.isVisible()) mBinding.layoutChanelFull.goneView()
        }

        mBinding.layoutLockPlayerViewFull.setOnLongClickListener {
            showLockScreen(isFullScreen = true, isLock = false)
            return@setOnLongClickListener true
        }

        mBinding.playerView.click {
            mBinding.playerView.useController = true
            try {
                val controllerView = mBinding.playerView.findViewById<View>(
                    androidx.media3.ui.R.id.exo_controller
                )
                val isControllerVisible = controllerView.visibility == View.VISIBLE
                if (isControllerVisible.not()) {
                    mBinding.playerView.showController()
                }
            } catch (_: Exception) {}
        }

        mBinding.playerViewFull.click {
            mBinding.playerViewFull.useController = true
            try {
                val controllerView = mBinding.playerViewFull.findViewById<View>(
                    androidx.media3.ui.R.id.exo_controller
                )
                val isControllerVisible = controllerView.visibility == View.VISIBLE
                if (isControllerVisible.not()) {
                    mBinding.playerViewFull.showController()
                }
            } catch (_: Exception) {}
            mBinding.layoutChanelFull.goneView()
        }

        mBinding.btnToolbarBack.click {
            onActivityBackPressed()
        }

        mBinding.etSearch.doOnTextChanged { text, start, before, count ->
            tvChanel?.let {
                channelViewModel.onEvent(ChannelEvent.SearchChanelByKeyword(this, it, text.toString(), groupType, filterType))
            }
        }

        mBinding.etSearchFull.doOnTextChanged { text, start, before, count ->
            tvChanel?.let {
                channelViewModel.onEvent(ChannelEvent.SearchChanelByKeyword(this, it, text.toString(), groupType, filterType))
            }
        }
    }

    private fun initAdapter() {
        channelAdapter = TVChannelAdapter(tvChanel?.url ?: "")
        mBinding.rcvChannel.adapter = channelAdapter
        mBinding.rcvChannelFull.adapter = channelAdapter

        channelAdapter.onClickTVChannel = { it, position, isHasScroll ->
            initPlayerForUrl(it)
            channelAdapter.updateSelectedItem(it)
            if (isHasScroll) {
                scrollToPosition(position)
            }
        }

        channelAdapter.onChangePositionSelected = { index, total, isHasScroll ->
            tvIndex?.text = getString(R.string.cast_index_value, index.toString(), total.toString())
            tvIndexFull?.text = getString(R.string.cast_index_value, index.toString(), total.toString())
            if (isHasScroll) scrollToPosition(index - 1)
        }
    }

    private fun isPictureInPictureSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }


    private fun enterPip() {
        mBinding.layoutChanel.goneView()
        mBinding.frBanner.visibility = View.GONE

        if (isPictureInPictureSupported()) {
            val aspectRatio = Rational(
                mBinding.playerView.width.takeIf { it > 0 } ?: 16,
                mBinding.playerView.height.takeIf { it > 0 } ?: 9
            )
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(params)
            isInPIP = true
        } else {
            showToastById(R.string.cast_pip_not_support)
        }
    }


    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            mBinding.playerView.useController = false
            mBinding.playerView.hideController()
            mBinding.layoutChanel.visibility = View.GONE
            mBinding.frBanner.visibility = View.GONE
        } else {
            isInPIP = false
            mBinding.playerView.useController = true
            mBinding.playerView.showController()
            mBinding.layoutChanel.visibility = View.VISIBLE
            mBinding.frBanner.visibility = View.VISIBLE
        }
    }

    private fun scrollToPosition(position: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                mBinding.rcvChannel.smoothScrollToPosition(position)
                mBinding.rcvChannelFull.smoothScrollToPosition(position)
            } catch (_: Exception) {
            }
        }, 100L)
    }

    private fun updateDataChannelState(state: DataState<TVChannel>) {
        when (state) {
            is DataState.Success -> {
                channelAdapter.submitData(state.data)
                mBinding.layoutEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                mBinding.layoutEmptyFull.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
            }
            else -> {}
        }
    }

    @UnstableApi
    private fun initPlayerForUrl(
        tvChannel: TVChannel,
        referrer: String = DEFAULT_REFERRER,
        userAgent: String = DEFAULT_USER_AGENT,
    ) {
        releasePlayer()
        initChanelInformation(tvChannel)
        tvChannel.connectedTime = System.currentTimeMillis().toString()
        CoroutineScope(Dispatchers.IO).launch {
            homeViewModel.updateChannel(tvChannel)
        }
        try {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(mapOf("Referer" to referrer))
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)
            
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
                this,
                httpDataSourceFactory
            )

            val mediaItem = createMediaItemWithMimeType(tvChannel.url)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            mBinding.playerView.controllerShowTimeoutMs = 0
            mBinding.playerView.useController = true
            mBinding.playerView.showController()

            mBinding.playerViewFull.controllerShowTimeoutMs = 0
            mBinding.playerViewFull.useController = true
            mBinding.playerViewFull.showController()

            mBinding.playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                handlerController?.removeCallbacks(runnableController)
                mBinding.btnFullScreenPersistent.visibility = if (visibility == View.VISIBLE || isFullScreen) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                if (visibility == View.VISIBLE) {
                    updateStateVideoController(true)
                    mBinding.root.findViewById<ImageButton>(R.id.btnPlayOverlay)?.goneView()
                    btnCast?.visibleView()
                    mBinding.playerView.findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.visibleView()
                    handlerController?.postDelayed(runnableController, TIME_SHOW_CONTROLLER)
                }
            })
            mBinding.playerViewFull.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            })
            if (!isFullScreen) {
                handlerController?.postDelayed(runnableController, TIME_SHOW_CONTROLLER)
            }

            updateStateVideoController(false)
            player = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build()
                .also { exoPlayer ->
                    if (isFullScreen.not()) mBinding.playerView.player = exoPlayer
                    else mBinding.playerViewFull.player = exoPlayer
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    castHelper?.setupPlayer(exoPlayer, mediaItem)
                }

            player?.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    Log.d(TAG, "onPlaybackStateChanged $state")

                    when (state) {

                        Player.STATE_IDLE -> {

                        }

                        Player.STATE_BUFFERING -> {
                            updateStateVideoController(false)
                        }

                        Player.STATE_READY -> {

                            initActionForController(true)
                            updateStateVideoController(true)
                            if (!isFullScreen) {
                                handlerController?.removeCallbacks(runnableController)
                                handlerController?.postDelayed(runnableController, TIME_SHOW_CONTROLLER)
                            }
                        }

                        Player.STATE_ENDED -> {
                            if (tvChannel.logo.isBlank()) {
                                try {
                                    player?.seekTo(0)
                                    player?.pause()
                                } catch (_: Exception) {}
                            }
                            initActionForController(tvChannel.logo.isNotBlank())
                            updateStateVideoController(true)
                            if (tvChannel.logo.isNotBlank()) showVideoEndDialog()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    btnToggle?.isActivated = isPlaying
                    btnToggleFull?.isActivated = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    updateStateVideoController(true)
                    initActionForController(true)
                    Log.d("Kai", "onPlayerError: ")
                    showVideoErrorDialog()
                }
            })
        } catch (ex: Exception) {
            showVideoErrorDialog()
        }
    }

    private fun initChanelInformation(tvChannel: TVChannel) {
        mBinding.tvToolbarTitle.text = tvChannel.name
        tvName?.text = tvChannel.name
        btnToggle?.isActivated = false
        btnFullScreen?.isActivated = false
        btnFavorite?.isSelected = tvChannel.isFavorite

        tvNameFull?.text = tvChannel.name
        btnToggleFull?.isActivated = false
        btnFullScreenFull?.isActivated = false
        btnFavoriteFull?.isSelected = tvChannel.isFavorite
    }

    private fun updateStateVideoController(isShow: Boolean) {
        Log.d("Kai", "updateStateVideoController: $isShow")
        btnToggle?.visibility = if (isShow) View.VISIBLE else View.GONE
        btnPrev?.visibility = if (isShow) View.VISIBLE else View.GONE
        btnNext?.visibility = if (isShow) View.VISIBLE else View.GONE

        btnToggleFull?.visibility = if (isShow) View.VISIBLE else View.GONE
        btnPrevFull?.visibility = if (isShow) View.VISIBLE else View.GONE
        btnNextFull?.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    private fun initViewIdForController() {
        btnClose = mBinding.playerView.findViewById(R.id.btnClose)
        btnFavorite = mBinding.playerView.findViewById(R.id.btnFavorite)
        tvName = mBinding.playerView.findViewById(R.id.tvName)
        tvIndex = mBinding.playerView.findViewById(R.id.tvIndex)
        btnPrev = mBinding.playerView.findViewById(R.id.btnPrev)
        btnToggle = mBinding.playerView.findViewById(R.id.btnToggle)
        btnNext = mBinding.playerView.findViewById(R.id.btnNext)

        btnCast = mBinding.playerView.findViewById(R.id.btnCast)
        btnPip = mBinding.playerView.findViewById(R.id.btnPip)
        btnTimer = mBinding.playerView.findViewById(R.id.btnTimer)
        btnLock = mBinding.playerView.findViewById(R.id.btnLock)
        btnFullScreen = mBinding.playerView.findViewById(R.id.btnFullScreen)

        mediaRouteButton = mBinding.playerView.findViewById(R.id.mediaRouteButton)

        btnCloseFull = mBinding.playerViewFull.findViewById(R.id.btnCloseFull)
        btnFavoriteFull = mBinding.playerViewFull.findViewById(R.id.btnFavoriteFull)
        tvNameFull = mBinding.playerViewFull.findViewById(R.id.tvNameFull)
        tvIndexFull = mBinding.playerViewFull.findViewById(R.id.tvIndexFull)
        btnPrevFull = mBinding.playerViewFull.findViewById(R.id.btnPrevFull)
        btnToggleFull = mBinding.playerViewFull.findViewById(R.id.btnToggleFull)
        btnNextFull = mBinding.playerViewFull.findViewById(R.id.btnNextFull)

        btnCastFull = mBinding.playerViewFull.findViewById(R.id.btnCastFull)
        mediaRouteButtonFull = mBinding.playerViewFull.findViewById(R.id.mediaRouteButtonFull)
        btnPipFull = mBinding.playerViewFull.findViewById(R.id.btnPipFull)
        btnTimerFull = mBinding.playerViewFull.findViewById(R.id.btnTimerFull)
        btnLockFull = mBinding.playerViewFull.findViewById(R.id.btnLockFull)
        btnFullScreenFull = mBinding.playerViewFull.findViewById(R.id.btnFullScreenFull)

        btnListFull = mBinding.playerViewFull.findViewById(R.id.btnListFull)

        mBinding.root.findViewById<ImageButton>(R.id.btnPlayOverlay)?.setOnClickListener {
            togglePlayer()
            mBinding.playerView.showController()
        }

        mBinding.btnFullScreenPersistent.setOnClickListener {
            switchPlayerView(true)
        }

        btnClose?.click {
            onActivityBackPressed()
        }

        btnCloseFull?.click {
            switchPlayerView(false)
        }

        btnListFull?.click {
            if (mBinding.layoutChanelFull.isVisible()) mBinding.layoutChanelFull.goneView()
            else mBinding.layoutChanelFull.visibleView()
        }

        btnTimer?.click {
            showTimerDialog()
        }

        btnTimerFull?.click {
            showTimerDialog()
        }

        mBinding.playerViewFull.click {
            if (mBinding.layoutChanelFull.isVisible()) mBinding.layoutChanelFull.goneView()
        }

        btnPip?.click {
            enterPip()
        }

        btnPipFull?.click {
            switchPlayerView(false)
            enterPip()
        }


        btnFavorite?.click {
            tvChanel?.let { chanel->
                chanel.isFavorite = chanel.isFavorite.not()
                lifecycleScope.launch {
                    btnFavorite?.isSelected = chanel.isFavorite
                    homeViewModel.updateChannelFavorite(chanel, chanel.isFavorite)
                    showToastById(if (chanel.isFavorite) R.string.cast_added_to_favorites else R.string.cast_removed_from_favorites)
                }
            }
        }

        btnFavoriteFull?.click {
            tvChanel?.let { chanel->
                chanel.isFavorite = chanel.isFavorite.not()
                lifecycleScope.launch {
                    btnFavoriteFull?.isSelected = chanel.isFavorite
                    homeViewModel.updateChannelFavorite(chanel, chanel.isFavorite)
                    showToastById(if (chanel.isFavorite) R.string.cast_added_to_favorites else R.string.cast_removed_from_favorites)
                }
            }
        }

        if (tvChanel?.logo?.isBlank() == true) {
            Log.d("Kai", "initViewIdForController: ")
            btnToggle?.click {
                togglePlayer()
            }

            btnToggleFull?.click {
                togglePlayer()
                mBinding.layoutChanelFull.goneView()
            }
        }
    }

    private fun initializeCast() {
        val currentPlayerView = if (isFullScreen) mBinding.playerViewFull else mBinding.playerView
        val currentMediaRouteButton = if (isFullScreen) mediaRouteButtonFull else mediaRouteButton
        
        castHelper = CastHelper(
            context = this,
            playerView = currentPlayerView,
            mediaRouteButton = currentMediaRouteButton
        ).apply {
            isInPictureInPictureMode = false
            onPlaybackStateChanged = { state, message ->
                if (!isInPictureInPictureMode) {
//                    showToastByString(message)
                }
            }
            onPlayerError = { error ->
                if (!isInPictureInPictureMode) {

//                    Log.d("Kai", "onPlayerError: 1")
//                    showVideoErrorDialog()
//                    showToastByString(error)
                }
            }
            onCastStarted = {
                showToastById(R.string.cast_casting)
            }
            onCastEnded = {
                showToastById(R.string.cast_stop_cast)
            }
            onCastLoading = { show ->
                showCastLoading(show)
            }
        }
//        castHelper?.initialize()
    }

    private fun showCastLoading(show: Boolean) {
        mBinding.layoutLoading.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun createMediaItemWithMimeType(url: String): MediaItem {
        val uri = android.net.Uri.parse(url)
        val mimeType = detectMimeType(url)
        
        return if (mimeType != null) {
            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .build()
        } else {
            MediaItem.fromUri(uri)
        }
    }

    private fun detectMimeType(url: String): String? {
        if (url.startsWith("content://")) {
            try {
                val uri = url.toUri()
                val mimeType = contentResolver.getType(uri)
                if (mimeType != null) {
                    return mimeType
                }
            } catch (e: Exception) {
                Log.e("PlayListActivity", "Failed to get mimeType from ContentResolver: ${e.message}")
            }
        }

        return when {
            url.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
            url.contains(".ism", ignoreCase = true) -> "application/vnd.ms-sstr+xml"
            url.contains(".mp4", ignoreCase = true) -> "video/mp4"
            url.contains(".webm", ignoreCase = true) -> "video/webm"
            url.contains(".mp3", ignoreCase = true) -> "audio/mpeg"
            url.contains(".m3u", ignoreCase = true) -> "application/x-mpegURL"
            else -> null
        }
    }

    private fun initActionForController(isHasAction: Boolean) {
        btnToggle?.click {
            if (isHasAction) {
                togglePlayer()
            }
        }

        btnToggleFull?.click {
            if (isHasAction) {
                togglePlayer()
            }
            mBinding.layoutChanelFull.goneView()
        }

        btnFullScreen?.click {
            if (isHasAction) {
                switchPlayerView(true)
            }
        }

        btnFullScreenFull?.click {
            if (isHasAction) {
                switchPlayerView(false)
            }
        }

        btnNext?.click {
            channelAdapter.onNextChanel()
        }

        btnNextFull?.click {
            channelAdapter.onNextChanel()
            mBinding.layoutChanelFull.goneView()
        }

        btnPrev?.click {
            channelAdapter.onPrevChanel()
        }

        btnPrevFull?.click {
            channelAdapter.onPrevChanel()
            mBinding.layoutChanelFull.goneView()
        }

        btnLock?.click {
            showLockScreen(isFullScreen = false, isLock = true)
        }

        btnLockFull?.click {
            showLockScreen(isFullScreen = true, isLock = true)
        }
    }

    private fun togglePlayer() {
        if (player?.isPlaying == true) {
            pausePlayer()
        } else {
            if (player?.currentPosition == player?.duration) {
                player?.seekTo(0)
                player?.play()
            } else playPLayer()
        }
    }

    private fun pausePlayer() {
        Log.d("Kai", "pausePlayer: ${player?.isPlaying}")
        try {
            if (player?.isPlaying == true) player?.pause()
        } catch (ex: Exception) {
            Log.d("Kai", "pausePlayer failed by: ${ex.message}")
        }
    }

    private fun playPLayer() {
        try {
            if (player?.isPlaying == false) player?.play()
        } catch (_: Exception) {}
    }

    private fun switchPlayerView(isFullScreen: Boolean) {
        try {
            player?.let { pl->
                requestedOrientation = if (isFullScreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                mBinding.playerView.visibility = if (isFullScreen) View.GONE else View.VISIBLE
                mBinding.toolbar.visibility = if (isFullScreen) View.GONE else View.VISIBLE
                mBinding.layoutChanel.visibility = if (isFullScreen) View.GONE else View.VISIBLE
                mBinding.layoutChanelFull.goneView()
                mBinding.playerViewFull.visibility = if (isFullScreen) View.VISIBLE else View.GONE
                val fromView = if (isFullScreen) mBinding.playerView else mBinding.playerViewFull
                val toView = if (isFullScreen) mBinding.playerViewFull else mBinding.playerView
                PlayerView.switchTargetView(pl, fromView, toView)

                castHelper?.let { helper ->
                    val newMediaRouteButton = if (isFullScreen) mediaRouteButtonFull else mediaRouteButton
                    helper.updatePlayerView(toView, newMediaRouteButton)
                }
                val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                if (isFullScreen) {
                    windowInsetsController.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    windowInsetsController.isAppearanceLightStatusBars = false
                } else {
                    windowInsetsController.show(android.view.WindowInsets.Type.statusBars())
                    windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())
                    windowInsetsController.isAppearanceLightStatusBars = true
                    window.statusBarColor = getColor(R.color.cast_cast_background_start)
                }
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isFullScreen) {
                    playerRootPaddingTop = mBinding.root.paddingTop
                    mBinding.root.setPadding(
                        mBinding.root.paddingLeft,
                        0,
                        mBinding.root.paddingRight,
                        mBinding.root.paddingBottom,
                    )
                } else {
                    mBinding.root.setPadding(
                        mBinding.root.paddingLeft,
                        playerRootPaddingTop,
                        mBinding.root.paddingRight,
                        mBinding.root.paddingBottom,
                    )
                }

                mBinding.playerView.controllerShowTimeoutMs = 0
                mBinding.playerView.useController = true
                mBinding.playerView.showController()

                mBinding.playerViewFull.controllerShowTimeoutMs = 0
                mBinding.playerViewFull.useController = true
                mBinding.playerViewFull.showController()

                this.isFullScreen = isFullScreen
                if (isFullScreen) {
                    mBinding.etSearch.setText("")
                } else {
                    mBinding.etSearchFull.setText("")
                }
            }
        } catch (_: Exception) {}
    }

    private fun showLockScreen(isFullScreen: Boolean, isLock: Boolean) {
        if (isFullScreen) {
            if (isLock) {
                mBinding.playerViewFull.useController = false
                mBinding.playerViewFull.hideController()
                mBinding.layoutLockPlayerViewFull.visibleView()
                mBinding.layoutLockContentFull.visibleView()
                showWithAutoHide(mBinding.layoutLockContentFull)
            } else {
                mBinding.playerViewFull.useController = true
                mBinding.playerViewFull.showController()
                cancelShowAnimation(mBinding.layoutLockContentFull)
                mBinding.layoutLockPlayerViewFull.goneView()
                mBinding.layoutLockContentFull.goneView()
            }
        } else {
            if (isLock) {
                mBinding.playerView.useController = false
                mBinding.playerView.hideController()
                mBinding.layoutLockPlayerView.visibleView()
                mBinding.layoutLockContent.visibleView()
                showWithAutoHide(mBinding.layoutLockContent)
            } else {
                mBinding.playerView.useController = true
                mBinding.playerView.showController()
                cancelShowAnimation(mBinding.layoutLockContent)
                mBinding.layoutLockPlayerView.goneView()
                mBinding.layoutLockContent.goneView()
            }
        }
    }

    private fun showWithAutoHide(view: View) {
        showLockJob?.cancel()
        showLockJob = lifecycleScope.launch {
            view.alpha = 1f
            view.visibility = View.VISIBLE
            delay(1000)
            val anim = view.animate()
                .alpha(0f)
                .setDuration(500)
            suspendCancellableCoroutine<Unit> { cont ->
                anim.withEndAction {
                    if (cont.isActive) cont.resume(Unit, null)
                }
            }
            view.visibility = View.GONE
            view.alpha = 1f
        }
    }

    private fun cancelShowAnimation(view: View) {
        showLockJob?.cancel()
        view.animate().cancel()
    }

    private fun closeTimerDialog() {
        try {
            val dialog = supportFragmentManager.findFragmentByTag(TAG_VIDEO_TIMER_DIALOG)
            if (dialog is VideoTimerDialog) dialog.dismiss()
        } catch (_: Exception) {}
    }

    private fun showTimerDialog() {
        closeTimerDialog()
        VideoTimerDialog.newInstance().show(supportFragmentManager, TAG_VIDEO_TIMER_DIALOG)
    }

    private fun closeVideoErrorDialog() {
        try {
            val dialog = supportFragmentManager.findFragmentByTag(TAG_VIDEO_ERROR_DIALOG)
            if (dialog is VideoErrorDialog) dialog.dismiss()
        } catch (_: Exception) {
        }
    }

    override fun whenInternetTurnOn() {
        closeNoInternetDialog()
        if (isOpenSetting) {
            isOpenSetting = false
            tvChanel?.let {
                initPlayerForUrl(it)
            }
        }
    }

    override fun whenInternetTurnOff() {
        pausePlayer()
        showNoInternetDialog()
    }

    private fun showVideoErrorDialog() {
        Log.d("Kai", "showVideoErrorDialog: ")
        pausePlayer()
        if (isNetwork()) {
            closeVideoErrorDialog()
            VideoErrorDialog.newInstance().show(supportFragmentManager, TAG_VIDEO_ERROR_DIALOG)
        } else showNoInternetDialog()
    }

    private fun closeVideoEndDialog() {
        try {
            val dialog = supportFragmentManager.findFragmentByTag(TAG_VIDEO_END_DIALOG)
            if (dialog is VideoEndDialog) dialog.dismiss()
        } catch (_: Exception) {
        }
    }

    private fun showVideoEndDialog() {
        closeVideoEndDialog()
        VideoEndDialog.newInstance().show(supportFragmentManager, TAG_VIDEO_END_DIALOG)
    }

    private fun releasePlayer() {
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (_: Exception) {
        }
    }

    override fun onNextChanelWhenError() {
        channelAdapter.onNextChanel()
    }

    override fun onNextChanelWhenEnd() {
        channelAdapter.onNextChanel()
    }

    override fun onSetTime(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        if (hour == currentHour && currentMinute == minute) {
            onActivityBackPressed()
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val delta = if (calendar.timeInMillis < System.currentTimeMillis()) 24 * 60 * 60 * 1000L else 0L
            iptvSharedPref.timeNeedPauseVideo = calendar.timeInMillis + delta
            handler?.removeCallbacksAndMessages(null)
            handler?.postDelayed(runnable, TIME_ONE_SECOND)
        }
    }

    override fun onClearTime() {
        iptvSharedPref.timeNeedPauseVideo = 0L
        handler?.removeCallbacksAndMessages(null)
    }

    override fun onStart() {
        super.onStart()
        Log.d("Kai", "onStart: ")
    }

    override fun onStop() {
        super.onStop()
        castHelper?.onStop()
        if (isInPictureInPictureMode) {
            isInPIP = false
            releasePlayer()
            handler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        isInPIP = false
        castHelper?.release()
        castHelper = null
        handler?.removeCallbacksAndMessages(null)
    }

    override fun onActivityBackPressed() {
        AdsManager.loadAndShowInterBack(this@PlayListActivity) {
            if (isHasShowAddWhenBack) {
                releasePlayer()
                castHelper?.release()
                castHelper = null
                handler?.removeCallbacksAndMessages(null)
                finish()
            } else finish()
        }
    }
}
