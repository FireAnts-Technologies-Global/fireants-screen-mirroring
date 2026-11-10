package com.hyper.castwave.ui.component.iptv.addchannel.file

import android.net.Uri
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.databinding.CastFragmentAddChannelFileBinding
import com.hyper.castwave.ui.bases.BaseFragment
import com.hyper.castwave.ui.bases.ext.showToastById
import com.hyper.castwave.ui.component.iptv.addchannel.helper.FileHelper
import com.hyper.castwave.ui.component.iptv.addchannel.helper.ImportStateHelper
import com.hyper.castwave.ui.component.iptv.addchannel.helper.ValidationHelper
import com.hyper.castwave.ui.component.iptv.addchannel.viewmodel.AddChannelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddChannelFileFragment : BaseFragment<CastFragmentAddChannelFileBinding>() {
    private val viewModel: AddChannelViewModel by activityViewModels()
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val fileName = FileHelper.getFileName(requireContext(), uri)
        if (!isM3UFile(fileName)) {
            requireContext().showToastById(R.string.cast_accepted_file_format)
            return@registerForActivityResult
        }
        handleFileSelected(uri, fileName)
    }

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_add_channel_file

    override fun initViews() {
        super.initViews()
        showLoading(false)
        setupClickListeners()
        observeImportState()
        updateAddButtonState()
    }

    private fun observeImportState() {
        ImportStateHelper.observeImportState(
            fragment = this,
            viewModel = viewModel,
            onLoading = { showLoading(true) },
            onSuccess = { channelCount, _ ->
                lifecycleScope.launch {
                    delay(400)
                    showLoading(false)
                    AdsManager.loadAndShowInterImportSuccessfulChannels(requireActivity()) {
                        ImportStateHelper.showSuccessDialog(
                            this@AddChannelFileFragment,
                            channelCount
                        )
                    }
                }
            },
            onError = { message ->
                lifecycleScope.launch {
                    delay(400)
                    showLoading(false)
                    ImportStateHelper.showErrorToast(this@AddChannelFileFragment, message)
                }
            },
            onNone = { updateAddButtonState() },
            buttonsToDisable = listOf(mBinding.btnAddPlaylist)
        )
    }

    private fun showLoading(show: Boolean) {
        mBinding.layoutLoading.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        mBinding.uploadZone.setOnClickListener { pickM3UFile() }

        mBinding.btnDeleteFile.setOnClickListener {
            removeSelectedFile()
        }

        mBinding.btnAddPlaylist.setOnClickListener {
            val playlistName = resolvePlaylistName()
            if (!ValidationHelper.validatePlaylistName(this, playlistName)) return@setOnClickListener
            if (!ValidationHelper.validateFile(this, selectedFileUri)) return@setOnClickListener

            viewModel.importFromFile(requireContext(), playlistName, selectedFileUri!!)
        }
    }

    private fun pickM3UFile() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    private fun handleFileSelected(uri: Uri, fileName: String) {
        selectedFileUri = uri
        selectedFileName = fileName

        mBinding.tvSelectedFileName.text = fileName
        mBinding.tvSelectedFileSize.text = FileHelper.formatFileSize(
            FileHelper.getFileSize(requireContext(), uri)
        )
        mBinding.selectedFileCard.visibility = View.VISIBLE
        updateAddButtonState()
    }

    private fun removeSelectedFile() {
        selectedFileUri = null
        selectedFileName = null
        mBinding.selectedFileCard.visibility = View.GONE
        updateAddButtonState()
    }

    private fun resolvePlaylistName(): String {
        val fileName = selectedFileName.orEmpty()
        val stem = fileName.substringBeforeLast('.', fileName)
        return stem.ifBlank { getString(R.string.cast_playlist_1) }
    }

    private fun isM3UFile(fileName: String): Boolean {
        return fileName.endsWith(".m3u", ignoreCase = true) ||
            fileName.endsWith(".m3u8", ignoreCase = true)
    }

    private fun updateAddButtonState() {
        val hasFile = selectedFileUri != null
        mBinding.btnAddPlaylist.isEnabled = true
        mBinding.btnAddPlaylist.isClickable = true
        mBinding.icAddPlaylistPlus.visibility = if (hasFile) View.GONE else View.VISIBLE
        mBinding.btnAddPlaylist.setBackgroundResource(R.drawable.cast_bg_import_primary)
    }
}
