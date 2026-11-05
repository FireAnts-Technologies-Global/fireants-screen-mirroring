package com.anrstudio.template.ui.component.iptv.addchannel.helper


import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anrstudio.template.R
import com.anrstudio.template.data.iptv.model.DataState
import com.anrstudio.template.ui.component.iptv.addchannel.viewmodel.AddChannelViewModel
import kotlinx.coroutines.launch

object ImportStateHelper {

    fun observeImportState(
        fragment: Fragment,
        viewModel: AddChannelViewModel,
        onLoading: (() -> Unit)? = null,
        onSuccess: ((Int, Int) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onNone: (() -> Unit)? = null,
        buttonsToDisable: List<View> = emptyList(),
    ) {
        fragment.lifecycleScope.launch {
            fragment.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Khi START -> bắt đầu collect
                viewModel.importState.collect { state ->

                    when (state) {
                        is DataState.Loading -> {
                            buttonsToDisable.forEach { it.isEnabled = false }
                            onLoading?.invoke()
                        }

                        is DataState.Success -> {
                            val result = state.data.firstOrNull()
                            if (result != null) {
                                onSuccess?.invoke(result.channelCount, result.groupCount)
                                viewModel.resetImportState()
                            }
                            buttonsToDisable.forEach { it.isEnabled = true }
                        }

                        is DataState.Error -> {
                            onError?.invoke(state.message)
                            buttonsToDisable.forEach { it.isEnabled = true }
                        }

                        is DataState.None -> {
                            onNone?.invoke()
                            buttonsToDisable.forEach { it.isEnabled = true }
                        }
                    }
                }
            }
        }
    }

    fun showSuccessToast(fragment: Fragment, channelCount: Int, groupCount: Int = 0) {
        var message = fragment.getString(R.string.cast_txt_playlist_added_successfully)
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()

        val activity = fragment.requireActivity()
        activity.setResult(android.app.Activity.RESULT_OK)
        activity.finish()
    }

    fun showErrorToast(fragment: Fragment, message: String) {
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}


