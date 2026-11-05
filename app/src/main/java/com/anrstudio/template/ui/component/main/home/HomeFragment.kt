package com.anrstudio.template.ui.component.main.home

import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdRemoteConfig
import com.anrstudio.template.ads.inter_home
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anrstudio.template.R
import com.anrstudio.template.navigation.AppNavigator
import com.anrstudio.template.databinding.CastFragmentHomeBinding
import com.anrstudio.template.databinding.CastItemHomeFeatureBinding
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.visibleView
import com.anrstudio.template.ads.populateNativeAdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    @Inject
    lateinit var navigator: AppNavigator

    private val viewModel: HomeViewModel by viewModels()

    private var _binding: CastFragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CastFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClicks()
        observeUiState()
        view.post { checkPermissions() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        loadNative()
        bindFeatureCard(
            binding = binding.cardFeaturePhoto,
            iconRes = R.drawable.cast_ic_home_feature_photo,
            titleRes = R.string.cast_home_cast_photos,
            descRes = R.string.cast_home_cast_photos_desc,
        )
        bindFeatureCard(
            binding = binding.cardFeatureVideo,
            iconRes = R.drawable.cast_ic_home_feature_video,
            titleRes = R.string.cast_home_cast_videos,
            descRes = R.string.cast_home_cast_videos_desc,
        )
        bindFeatureCard(
            binding = binding.cardFeatureImport,
            iconRes = R.drawable.cast_ic_home_feature_import,
            titleRes = R.string.cast_home_import_channel,
            descRes = R.string.cast_home_import_channel_desc,
        )
        bindFeatureCard(
            binding = binding.cardFeatureCast,
            iconRes = R.drawable.cast_ic_home_feature_mirror,
            titleRes = R.string.cast_cast_tv,
            descRes = R.string.cast_home_screen_mirroring_desc,
        )
    }

    private fun bindFeatureCard(
        binding: CastItemHomeFeatureBinding,
        iconRes: Int,
        titleRes: Int,
        descRes: Int,
    ) {
        binding.imgFeatureIcon.setImageResource(iconRes)
        binding.tvFeatureTitle.setText(titleRes)
        binding.tvFeatureDesc.setText(descRes)
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
            is HomeUiState.Content -> renderMirrorBanner(state.isMirroring)
        }
    }

    private fun renderMirrorBanner(isMirroring: Boolean) {
        if (isMirroring) {
            binding.tvCastBannerDesc.setText(R.string.cast_status_connected)
            binding.tvCastBannerDesc.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.cast_color_primary),
            )
        } else {
            binding.tvCastBannerDesc.setText(R.string.cast_home_cast_banner_desc)
            binding.tvCastBannerDesc.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.cast_color_tab_inactive),
            )
        }
    }

    private fun setupClicks() {
        binding.cardMirrorBanner.click {
            openWebCast()
        }
        binding.cardHowToUse.click {
            AdsManager.loadAndShowInterstitial(requireActivity(), AdRemoteConfig.inter_home) {
                navigator.openHowToUse(requireContext())
            }
        }
        binding.cardFeaturePhoto.root.click {
            openMediaWithPermission(MediaType.IMAGE)
        }
        binding.cardFeatureVideo.root.click {
            openMediaWithPermission(MediaType.VIDEO)
        }
        binding.cardFeatureImport.root.click {
            (requireActivity() as? com.anrstudio.template.ui.component.main.MainActivity)?.openChannelsTab()
        }
        binding.cardFeatureCast.root.click {
            openCastScreen()
        }
    }

    private fun openCastScreen() {
        AdsManager.loadAndShowInterstitial(requireActivity(), AdRemoteConfig.inter_home) {
            navigator.openCastScreen(requireContext())
        }
    }

    private fun openWebCast() {
        AdsManager.loadAndShowInterstitial(requireActivity(), AdRemoteConfig.inter_home) {
            navigator.openWebCast(requireContext())
        }
    }

    private fun loadNative() {
        AdsManager.nativeHomeAdLive.observe(viewLifecycleOwner) { ad ->
            if (ad != null) {
                binding.frAdsNative.visibleView()
                populateNativeAdView(requireActivity(), ad, binding.frAdsNative,
                    binding.shimmerContainerNative.shimmerNativeSmall)
            }
        }
        AdsManager.nativeHomeAdStateLive.observe(viewLifecycleOwner) { state ->
            if (state == AdsManager.NativeAdLoadState.FAILED) binding.frAdsNative.goneView()
        }
        AdsManager.loadNativeHome(requireActivity(), R.layout.cast_layout_native_home)
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
                AdsManager.loadAndShowInterstitial(requireActivity(), AdRemoteConfig.inter_home) {
                    navigator.openMediaPicker(requireContext(), type)
                }
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
