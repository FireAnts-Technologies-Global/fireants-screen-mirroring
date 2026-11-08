package com.anrstudio.template.ui.component.webbrowser

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.anrstudio.template.databinding.CastLayoutBottomSheetVideoListBinding
import com.anrstudio.template.domain.model.ExtractedVideo
import com.anrstudio.template.ui.bases.BaseBottomSheetDialogFragment
import com.anrstudio.template.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoListBottomSheet(
    private val onVideoClick: (ExtractedVideo) -> Unit,
) : BaseBottomSheetDialogFragment<CastLayoutBottomSheetVideoListBinding>() {



    private val viewModel: WebBrowserViewModel by activityViewModels()
    private lateinit var adapter: ExtractedVideoAdapter

    override fun getLayoutFragment(): Int = R.layout.cast_layout_bottom_sheet_video_list

    override fun initViews() {
        super.initViews()
        adapter = ExtractedVideoAdapter { video ->
            onVideoClick(video)
            dismiss()
        }

        mBinding.recyclerViewVideos.layoutManager = LinearLayoutManager(requireContext())
        mBinding.recyclerViewVideos.adapter = adapter

    }

    override fun observerData() {
        super.observerData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.videos)
                }
            }
        }
    }

}
