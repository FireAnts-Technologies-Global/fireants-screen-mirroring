package com.hyper.castwave.ui.component.iptv.addchannel.helper


import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hyper.castwave.R
import com.hyper.castwave.data.iptv.model.DataState
import com.hyper.castwave.ui.component.iptv.addchannel.viewmodel.AddChannelViewModel
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

    fun showSuccessDialog(fragment: Fragment, channelCount: Int, groupCount: Int = 0) {
        val activity = fragment.requireActivity()
        val dialog = Dialog(activity)
        val content = LayoutInflater.from(activity).inflate(R.layout.cast_dialog_import_success, null)
        content.findViewById<View>(R.id.btnViewChannels).setOnClickListener {
            dialog.dismiss()
            activity.setResult(android.app.Activity.RESULT_OK)
            activity.finish()
        }
        dialog.setContentView(content)
        dialog.setCancelable(false)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
        }
        dialog.show()
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun showErrorToast(fragment: Fragment, message: String) {
        val context = fragment.requireContext()
        val localizedMessage = when (message) {
            "import_error_no_channels_playlist" -> context.getString(R.string.import_error_no_channels_playlist)
            "import_error_url" -> context.getString(R.string.import_error_url)
            "import_error_stream" -> context.getString(R.string.import_error_stream)
            "import_error_read_file" -> context.getString(R.string.import_error_read_file)
            "import_error_no_channels_file" -> context.getString(R.string.import_error_no_channels_file)
            "import_error_file" -> context.getString(R.string.import_error_file)
            "import_error_no_videos" -> context.getString(R.string.import_error_no_videos)
            "import_error_device" -> context.getString(R.string.import_error_device)
            else -> message
        }
        Toast.makeText(context, localizedMessage, Toast.LENGTH_SHORT).show()
    }
}


