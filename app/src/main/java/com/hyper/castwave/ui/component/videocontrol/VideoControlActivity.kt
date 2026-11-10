package com.hyper.castwave.ui.component.videocontrol

import com.hyper.castwave.ui.bases.ext.animateClick
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.hyper.castwave.R
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.databinding.CastActivityVideoControlBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VideoControlActivity : BaseActivity<CastActivityVideoControlBinding>() {

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, VideoControlActivity::class.java)
    }

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    private val viewModel: VideoControlViewModel by viewModels()
    private var isUserSeeking = false

    override fun getLayoutActivity(): Int = R.layout.cast_activity_video_control

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) =
        CastActivityVideoControlBinding.inflate(inflater)

    private fun stateFlow() = viewModel.uiState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectState(viewModel.events, ::handleEvent)
    }

    private fun setupViews() {
        Glide.with(this).load(R.drawable.cast_img_background).centerCrop().into(mBinding.imgBackground)
        
        mBinding.topBar.imgBack.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.visibility = View.VISIBLE
        mBinding.topBar.tvTitle.text = getString(R.string.cast_video_control)
        mBinding.topBar.imgCast.visibility = View.GONE
        mBinding.topBar.imgSwap.visibility = View.GONE
        mBinding.topBar.lnActionHome.visibility = View.GONE
        mBinding.topBar.tvTitleHome.visibility = View.GONE

        mBinding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = viewModel.uiState.value.duration
                    val position = (duration * progress / 100f).toLong()
                    mBinding.tvCurrentTime.text = formatTime(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val duration = viewModel.uiState.value.duration
                val position = (duration * (seekBar?.progress ?: 0) / 100f).toLong()
                viewModel.seekTo(position)
            }
        })

        mBinding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.setVolume(progress / 100f)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClicks() {
        mBinding.topBar.imgBack.animateClick {
            onBackPressed()
        }

        mBinding.btnPlayPause.animateClick {
            viewModel.togglePlayPause()
        }

        mBinding.btnStop.animateClick {
            viewModel.stop()
        }

        mBinding.btnDisconnect.animateClick {
            viewModel.disconnect()
        }
    }

    private fun renderState(state: VideoControlUiState) {
        mBinding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        mBinding.tvDeviceName.text = state.deviceName.ifEmpty { getString(R.string.cast_unknown_device) }
        mBinding.tvVideoTitle.text = state.title.ifEmpty { getString(R.string.cast_no_media_playing) }

        if (!state.thumbnailUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(state.thumbnailUrl)
                .placeholder(R.drawable.cast_img_error)
                .error(R.drawable.cast_img_error)
                .into(mBinding.imgThumbnail)
        } else {
            mBinding.imgThumbnail.setImageResource(R.drawable.cast_img_error)
        }

        mBinding.btnPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.cast_ic_pause else R.drawable.cast_ic_play
        )

        if (!isUserSeeking && state.duration > 0) {
            val progress = ((state.currentPosition.toFloat() / state.duration) * 100).toInt()
            mBinding.seekBar.progress = progress
        }

        mBinding.tvCurrentTime.text = formatTime(state.currentPosition)
        mBinding.tvDuration.text = formatTime(state.duration)

        mBinding.seekBarVolume.progress = (state.volume * 100).toInt()
    }

    private fun handleEvent(event: VideoControlEvent) {
        when (event) {
            is VideoControlEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            VideoControlEvent.NavigateBack -> {
                finish()
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
