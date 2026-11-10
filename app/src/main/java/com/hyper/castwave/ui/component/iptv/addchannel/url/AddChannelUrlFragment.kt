package com.hyper.castwave.ui.component.iptv.addchannel.url

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.databinding.CastFragmentAddChannelUrlBinding
import com.hyper.castwave.ui.bases.BaseFragment
import com.hyper.castwave.ui.bases.ext.isTikTokLink
import com.hyper.castwave.ui.bases.ext.isYouTubeLink
import com.hyper.castwave.ui.bases.ext.showToastById
import com.hyper.castwave.ui.component.iptv.addchannel.helper.ImportStateHelper
import com.hyper.castwave.ui.component.iptv.addchannel.helper.ValidationHelper
import com.hyper.castwave.ui.component.iptv.addchannel.viewmodel.AddChannelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddChannelUrlFragment : BaseFragment<CastFragmentAddChannelUrlBinding>() {

    private val viewModel: AddChannelViewModel by activityViewModels()
    private var suggestedPlaylistName: String = ""

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_add_channel_url

    override fun initViews() {
        super.initViews()
        showLoading(false)
        setupClickListeners()
        setupTextWatcher()
        observeImportState()
    }

    override fun observerData() {
        super.observerData()
        lifecycleScope.launch {
            viewModel.allChannelNames.collect { names ->
                suggestedPlaylistName = generateNextPlaylistName(names.distinct())
            }
        }
    }

    private fun generateNextPlaylistName(existingNames: List<String>): String {
        var index = 1
        var newName: String
        do {
            newName = "Playlist $index"
            index++
        } while (existingNames.contains(newName))
        return newName
    }

    private fun observeImportState() {
        ImportStateHelper.observeImportState(
            fragment = this,
            viewModel = viewModel,
            onLoading = { showLoading(true) },
            onSuccess = { channelCount, groupCount ->
                lifecycleScope.launch {
                    delay(400)
                    showLoading(false)
                    AdsManager.loadAndShowInterImportSuccessfulChannels(requireActivity()) {
                        ImportStateHelper.showSuccessDialog(
                            this@AddChannelUrlFragment,
                            channelCount,
                            groupCount,
                        )
                    }
                }
            },
            onError = { message ->
                lifecycleScope.launch {
                    delay(400)
                    showLoading(false)
                    ImportStateHelper.showErrorToast(this@AddChannelUrlFragment, message)
                }
            },
            buttonsToDisable = listOf(mBinding.btnAddPlaylist)
        )
    }

    private fun showLoading(show: Boolean) {
        mBinding.layoutLoading.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        mBinding.btnAddPlaylist.setOnClickListener {
            val playlistName = mBinding.etPlaylistName.text?.toString()?.trim().orEmpty()
                .ifBlank { suggestedPlaylistName.ifBlank { getString(R.string.cast_playlist_1) } }
            val playlistUrl = mBinding.etPlaylistUrl.text?.toString()?.trim().orEmpty()

            if (!ValidationHelper.validatePlaylistName(this, playlistName)) return@setOnClickListener
            if (!ValidationHelper.validateUrlM4UM4U8(this, playlistUrl)) return@setOnClickListener

            viewModel.importFromUrl(playlistName, playlistUrl)
        }
    }

    private fun setupTextWatcher() {
        mBinding.etPlaylistName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        mBinding.etPlaylistUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s.toString().isYouTubeLink()) {
                    requireContext().showToastById(R.string.cast_youtube_link)
                    mBinding.etPlaylistUrl.setText("")
                } else if (s.toString().isTikTokLink()) {
                    requireContext().showToastById(R.string.cast_tiktok_link)
                    mBinding.etPlaylistUrl.setText("")
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
