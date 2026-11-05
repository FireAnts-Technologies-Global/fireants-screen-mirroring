package com.anrstudio.template.ui.component.webbrowser

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.anrstudio.template.databinding.CastLayoutBottomSheetVideoListBinding
import com.anrstudio.template.domain.model.ExtractedVideo
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoListBottomSheet(
    private val onVideoClick: (ExtractedVideo) -> Unit,
) : BottomSheetDialogFragment() {



    private var _binding: CastLayoutBottomSheetVideoListBinding? = null
    private val binding get() = requireNotNull(_binding)
    
    private val viewModel: WebBrowserViewModel by activityViewModels()
    private lateinit var adapter: ExtractedVideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CastLayoutBottomSheetVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExtractedVideoAdapter { video ->
            onVideoClick(video)
            dismiss()
        }
        
        binding.recyclerViewVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVideos.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.videos)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
