package com.hyper.castwave.ui.component.webcast

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.databinding.CastActivityWebCastBinding
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.ext.maybeShowRateAfterFirstWebMirror
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebCastActivity : BaseActivity<CastActivityWebCastBinding>() {

    private val viewModel: WebCastViewModel by viewModels()

    private var wasMirroring = false

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        viewModel.start(result.resultCode, data)
    }

    override fun getLayoutActivity(): Int = R.layout.cast_activity_web_cast

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) =
        CastActivityWebCastBinding.inflate(inflater)

    private fun stateFlow() = viewModel.uiState

    private fun setupViews() = Unit

    private fun setupClicks() {
        mBinding.btnBack.setOnClickListener { onBackPressed() }
        mBinding.tvStartScreen.setOnClickListener {
            if (viewModel.uiState.value.isMirroring) {
                viewModel.stop()
            } else {
                AdsManager.loadAndShowRewardScreenMirroring(
                    activity = this,
                    onUserEarnedReward = { requestProjectionConsent() },
                    onFail = { requestProjectionConsent() },
                )
            }
        }
    }

    private fun renderState(state: WebCastUiState) {
        mBinding.tvIpWifi.text = state.ipUrl
        mBinding.tvStartScreenLabel.text = getString(
            if (state.isMirroring) R.string.cast_screen_mirror_stop else R.string.cast_screen_mirror_start,
        )
        mBinding.tvStartScreen.setBackgroundResource(
            if (state.isMirroring) {
                R.drawable.cast_bg_import_add_playlist_disabled
            } else {
                R.drawable.cast_bg_import_add_playlist_enabled
            }
        )
        if (state.isMirroring && !wasMirroring) {
            maybeShowRateAfterFirstWebMirror()
        }
        wasMirroring = state.isMirroring
    }

    private fun requestProjectionConsent() {
        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, WebCastActivity::class.java)
    }
}
