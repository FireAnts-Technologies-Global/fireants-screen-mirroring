package com.anrstudio.template.ui.component.main.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentHomeBinding
import com.anrstudio.template.databinding.CastItemHomeFeatureBinding
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.navigation.AppNavigator
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.bases.ext.click
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<CastFragmentHomeBinding>() {

    @Inject
    lateinit var navigator: AppNavigator

    private val viewModel: HomeViewModel by viewModels()

    private var pendingMediaType: MediaType? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) showNotificationRationale()
    }

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val type = pendingMediaType ?: return@registerForActivityResult
        pendingMediaType = null
        if (results.values.all { it }) {
            navigator.openMediaPicker(requireContext(), type)
        }
    }

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_home

    override fun observerData() {
        super.observerData()
        observeUiState()
        mBinding.root.post { checkPermissions() }
    }

    override fun initViews() {
        super.initViews()
        bindFeatureCard(
            itemBinding = mBinding.cardFeaturePhoto,
            iconRes = R.drawable.home_photo,
            titleRes = R.string.cast_home_photo,
            tileColor = ContextCompat.getColor(requireContext(), R.color.cast_home_feature_photo),
        )
        bindFeatureCard(
            itemBinding = mBinding.cardFeatureVideo,
            iconRes = R.drawable.home_video,
            titleRes = R.string.cast_home_video,
            tileColor = ContextCompat.getColor(requireContext(), R.color.cast_home_feature_video),
        )
        bindFeatureCard(
            itemBinding = mBinding.cardFeatureImport,
            iconRes = R.drawable.home_file,
            titleRes = R.string.cast_home_import_m3u,
            tileColor = ContextCompat.getColor(requireContext(), R.color.cast_home_feature_import),
        )
        bindFeatureCard(
            itemBinding = mBinding.cardFeatureCast,
            iconRes = R.drawable.home_web,
            titleRes = R.string.cast_home_browser_cast,
            tileColor = ContextCompat.getColor(requireContext(), R.color.cast_home_feature_browser),
        )
    }

    private fun bindFeatureCard(
        itemBinding: CastItemHomeFeatureBinding,
        iconRes: Int,
        titleRes: Int,
        tileColor: Int,
    ) {
        itemBinding.imgFeatureIcon.setImageResource(iconRes)
        itemBinding.imgFeatureIcon.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(tileColor)
            cornerRadius = 10f * resources.displayMetrics.density
        }
        itemBinding.tvFeatureTitle.setText(titleRes)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    private fun renderState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Content -> renderHomeState(state)
        }
    }

    private fun renderHomeState(state: HomeUiState.Content) {
        mBinding.tvConnectionTitle.text = if (state.isConnected) {
            state.connectedDeviceName ?: getString(R.string.cast_status_connected)
        } else {
            getString(R.string.cast_home_no_tv)
        }
        mBinding.tvConnectionSubtitle.text = if (state.isConnected) {
            getString(R.string.cast_home_connected_ready)
        } else {
            getString(R.string.cast_home_choose_device)
        }
        mBinding.tvConnectionSubtitle.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.isConnected) R.color.cast_home_connected_green else R.color.cast_home_text_secondary,
            ),
        )
        mBinding.btnConnectionAction.text = getString(
            if (state.isConnected) R.string.cast_disconnect else R.string.cast_connect,
        )
        mBinding.btnConnectionAction.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.isConnected) R.color.cast_home_connected_red else R.color.cast_color_nav_selected,
            ),
        )
        mBinding.btnConnectionAction.setBackgroundResource(
            if (state.isConnected) {
                R.drawable.cast_bg_home_connection_action_connected
            } else {
                R.drawable.cast_bg_home_connection_action
            },
        )
        mBinding.tvCastBannerDesc.text = if (state.isConnected) {
            getString(
                R.string.cast_home_start_mirroring,
                state.connectedDeviceName ?: getString(R.string.cast_home_tv)
            )
        } else {
            getString(R.string.cast_home_share_screen)
        }
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnConnectionAction.click {
            if (viewModel.uiState.value.let { it is HomeUiState.Content && it.isConnected }) {
                viewModel.disconnect()
            } else {
                openCastScreen()
            }
        }
        mBinding.cardMirrorBanner.click {
            openCastScreen()
        }
        mBinding.cardHowToUse.click {
            navigator.openHowToUse(requireContext())
        }
        mBinding.cardFeaturePhoto.root.click {
            openMediaWithPermission(MediaType.IMAGE)
        }
        mBinding.cardFeatureVideo.root.click {
            openMediaWithPermission(MediaType.VIDEO)
        }
        mBinding.cardFeatureImport.root.click {
            (requireActivity() as? com.anrstudio.template.ui.component.main.MainActivity)?.openChannelsTab()
        }
        mBinding.cardFeatureCast.root.click {
            openCastScreen()
        }
    }

    private fun openCastScreen() {
        navigator.openCastScreen(requireContext())
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED -> return

            isPermanentlyDenied(permission) -> showNotificationRationale()
            else -> {
                markAsked(permission)
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openMediaWithPermission(type: MediaType) {
        val permissions = mediaPermissionsFor(type)
        when {
            permissions.all {
                ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
            } -> {
                navigator.openMediaPicker(requireContext(), type)
            }

            permissions.any { isPermanentlyDenied(it) } -> showMediaSettingsDialog()
            else -> {
                pendingMediaType = type
                permissions.forEach { markAsked(it) }
                mediaPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun mediaPermissionsFor(type: MediaType): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            when (type) {
                MediaType.IMAGE -> Manifest.permission.READ_MEDIA_IMAGES
                MediaType.VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
                MediaType.AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
            },
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun showNotificationRationale() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val permanentlyDenied = isPermanentlyDenied(permission)
        PermissionRationaleDialog(
            context = requireContext(),
            iconRes = R.drawable.cast_ic_permission_notification,
            title = getString(R.string.cast_permission_notification_title),
            message = getString(R.string.cast_permission_notification_message),
            allowText = if (permanentlyDenied) {
                getString(R.string.cast_go_to_setting)
            } else {
                getString(R.string.cast_permission_allow)
            },
            denyText = getString(R.string.cast_permission_not_now),
            onAllow = {
                if (permanentlyDenied) {
                    openAppSettings()
                } else {
                    markAsked(permission)
                    notificationPermissionLauncher.launch(permission)
                }
            },
            onDeny = {},
        ).show()
    }

    private fun showMediaSettingsDialog() {
        PermissionRationaleDialog(
            context = requireContext(),
            iconRes = R.drawable.cast_ic_image,
            title = getString(R.string.cast_permission_media_title),
            message = getString(R.string.cast_permission_media_message),
            allowText = getString(R.string.cast_go_to_setting),
            denyText = getString(R.string.cast_permission_not_now),
            onAllow = { openAppSettings() },
            onDeny = {},
        ).show()
    }

    private fun isPermanentlyDenied(permission: String): Boolean {
        val hasAsked = requireContext()
            .getSharedPreferences(PREFS_PERMISSIONS, android.content.Context.MODE_PRIVATE)
            .getBoolean(permission, false)
        return hasAsked &&
            !shouldShowRequestPermissionRationale(permission) &&
            ContextCompat.checkSelfPermission(requireContext(), permission) !=
            PackageManager.PERMISSION_GRANTED
    }

    private fun markAsked(permission: String) {
        requireContext()
            .getSharedPreferences(PREFS_PERMISSIONS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(permission, true)
            .apply()
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", requireContext().packageName, null)),
        )
    }

    companion object {
        private const val PREFS_PERMISSIONS = "prefs_permissions"
    }
}
