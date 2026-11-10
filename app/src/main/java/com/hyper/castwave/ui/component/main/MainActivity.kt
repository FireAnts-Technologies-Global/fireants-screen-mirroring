package com.hyper.castwave.ui.component.main

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.hyper.castwave.BuildConfig
import com.hyper.castwave.R
import com.hyper.castwave.ads.RemoteConfigUtils
import com.hyper.castwave.data.model.ForceUpdateConfig
import com.hyper.castwave.databinding.CastActivityHomeBinding
import com.hyper.castwave.databinding.CastItemBottomNavTabBinding
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.ConsentHandler
import com.hyper.castwave.ui.bases.StatusBarConfig
import com.hyper.castwave.ui.bases.UiState
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.component.iptv.channel.ChannelFragment
import com.hyper.castwave.ui.component.main.dialog.ForceUpdateDialog
import com.hyper.castwave.ui.component.main.home.HomeFragment
import com.hyper.castwave.ui.component.main.setting.SettingFragment
import com.hyper.castwave.utils.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : BaseActivity<CastActivityHomeBinding>() {

    private var selectedHomeTab = HomeTab.HOME

    override val statusBarConfig: StatusBarConfig
        get() = StatusBarConfig(
            isVisible = true,
            applyPadding = false,
            barColor = Color.TRANSPARENT,
            lightIcons = selectedHomeTab == HomeTab.CHANNELS,
        )

    override fun requestWindow() {
        super.requestWindow()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
        }
    }


    private lateinit var consentHandler: ConsentHandler
    private val delayHandler = Handler(Looper.getMainLooper())
    private var delayRunnable: Runnable? = null
    private lateinit var forceUpdateDialog: ForceUpdateDialog
    private var cachedForceUpdateConfig: ForceUpdateConfig? = null



    private lateinit var homeFragment: HomeFragment
    private lateinit var channelFragment: ChannelFragment
    private lateinit var settingFragment: SettingFragment
    private lateinit var activeFragment: Fragment

    private val hostState = MutableStateFlow(HomeHostUiState)

    private val networkMonitor by lazy { com.hyper.castwave.utils.CastNetworkMonitor(this) }

    override fun getLayoutActivity(): Int = R.layout.cast_activity_home

    override fun initViews() {
        super.initViews()
        networkMonitor.attach()
        forceUpdateDialog = ForceUpdateDialog(this)
        initConsentHandler()
        checkConsentStatus()
        maybeShowForceUpdateDialog()
        setupViews()
        setupClicks()
        collectState(stateFlow(), ::renderState)
    }

    override fun inflateBinding(inflater: LayoutInflater) = CastActivityHomeBinding.inflate(inflater)

    private fun stateFlow(): StateFlow<HomeHostUiState> = hostState.asStateFlow()

    private fun renderState(state: HomeHostUiState) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFragments(savedInstanceState)
        setupBottomNav()
        handleInitialTab()
    }

    private fun setupViews() = Unit

    private fun setupClicks() = Unit

    private fun initFragments(savedInstanceState: Bundle?) {
        homeFragment = findOrCreateFragment(TAG_HOME) { HomeFragment() }
        channelFragment = findOrCreateFragment(TAG_CHANNELS) { ChannelFragment() }
        settingFragment = findOrCreateFragment(TAG_SETTING) { SettingFragment() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, settingFragment, TAG_SETTING)
                .hide(settingFragment)
                .add(R.id.fragmentContainer, channelFragment, TAG_CHANNELS)
                .hide(channelFragment)
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .commit()
            activeFragment = homeFragment
        } else {
            activeFragment = when (savedInstanceState.getString(KEY_SELECTED_TAB)) {
                TAB_CHANNELS -> channelFragment
                TAB_SETTING -> settingFragment
                else -> homeFragment
            }
        }
    }

    private inline fun <reified T : Fragment> findOrCreateFragment(
        tag: String,
        creator: () -> T,
    ): T {
        return supportFragmentManager.findFragmentByTag(tag) as? T ?: creator()
    }

    private fun setupBottomNav() {
        mBinding.bottomNav.tabHome.root.click { switchTab(HomeTab.HOME) }
        mBinding.bottomNav.tabChannels.root.click { switchTab(HomeTab.CHANNELS) }
        mBinding.bottomNav.tabSetting.root.click { switchTab(HomeTab.SETTING) }

        updateBottomNav(HomeTab.HOME)
    }

    private fun handleInitialTab() {
        when (intent.getStringExtra(EXTRA_TAB)) {
            TAB_CHANNELS -> switchTab(HomeTab.CHANNELS)
            TAB_SETTING -> switchTab(HomeTab.SETTING)
            else -> Unit
        }
    }

    fun openChannelsTab() {
        switchTab(HomeTab.CHANNELS)
    }

    private fun switchTab(tab: HomeTab) {
        val target = when (tab) {
            HomeTab.HOME -> homeFragment
            HomeTab.CHANNELS -> channelFragment
            HomeTab.SETTING -> settingFragment
        }
        if (target == activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
        updateBottomNav(tab)
    }

    private fun updateBottomNav(selectedTab: HomeTab) {
        selectedHomeTab = selectedTab
        ViewCompat.requestApplyInsets(mBinding.root)
        mBinding.root.post {
            WindowInsetsControllerCompat(window, mBinding.root)
                .isAppearanceLightStatusBars = selectedTab == HomeTab.CHANNELS
        }
        bindTab(
            tabBinding = mBinding.bottomNav.tabHome,
            isSelected = selectedTab == HomeTab.HOME,
            icon = R.drawable.ic_nav_home_figma,
            labelRes = R.string.cast_tab_home,
        )
        bindTab(
            tabBinding = mBinding.bottomNav.tabChannels,
            isSelected = selectedTab == HomeTab.CHANNELS,
            icon = R.drawable.ic_nav_channels_figma,
            labelRes = R.string.cast_tab_channels,
            selectedBackground = R.drawable.cast_bg_channels_nav_selected,
            selectedColor = R.color.cast_white,
        )
        bindTab(
            tabBinding = mBinding.bottomNav.tabSetting,
            isSelected = selectedTab == HomeTab.SETTING,
            icon = R.drawable.ic_nav_settings_figma,
            labelRes = if (selectedTab == HomeTab.CHANNELS) {
                R.string.cast_settings
            } else {
                R.string.cast_tab_setting
            },
        )
    }

    private fun bindTab(
        tabBinding: CastItemBottomNavTabBinding,
        isSelected: Boolean,
        icon: Int,
        labelRes: Int,
        selectedBackground: Int = R.drawable.cast_bg_home_nav_selected,
        selectedColor: Int = R.color.cast_color_nav_selected,
    ) {
        tabBinding.root.setBackgroundResource(
            if (isSelected) selectedBackground else android.R.color.transparent,
        )
        tabBinding.imgTabIcon.setImageResource(icon)
        tabBinding.imgTabIcon.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (isSelected) selectedColor else R.color.cast_color_tab_inactive,
            ),
        )
        tabBinding.tvTabLabelGradient.setText(labelRes)
        tabBinding.tvTabLabelGradient.setGradientColors(
            selectedColor,
            selectedColor,
        )
        tabBinding.tvTabLabelGradient.setTextColor(
            ContextCompat.getColor(this, selectedColor),
        )
        tabBinding.tvTabLabel.setText(labelRes)
        tabBinding.tvTabLabelGradient.visibility = if (isSelected) View.VISIBLE else View.GONE
        tabBinding.tvTabLabel.visibility = if (isSelected) View.GONE else View.VISIBLE
        tabBinding.viewTabIndicator.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val tab = when (activeFragment) {
            is ChannelFragment -> TAB_CHANNELS
            is SettingFragment -> TAB_SETTING
            else -> TAB_HOME
        }
        outState.putString(KEY_SELECTED_TAB, tab)
    }

    private fun initConsentHandler() {
        consentHandler = ConsentHandler(
            activity = this,
            appSharedPref = appSharedPref,
            trackingSuffix = 2,
            onConsentFlowCompleted = { Timber.d("Consent flow completed") },
            onConsentSuccess = { canPersonalized ->
                if (canPersonalized) {
                    Routes.startSplashActivity(this)
                    finish()
                }
            },
            onNotUsingAdConsent = {
                appSharedPref.isUserGlobal = true
            }
        )
    }

    private fun checkConsentStatus() {
        if (appSharedPref.isConfirmConsent.not() && appSharedPref.isUserGlobal.not()) {
            delayShowConsentDialog()
        }
    }

    private fun delayShowConsentDialog() {
        if (!RemoteConfigUtils.getOnShowDialogConsent()) {
            return
        }
        delayRunnable = Runnable {
            consentHandler.requestConsent()
        }
        delayHandler.postDelayed(delayRunnable!!, 5000L)
    }



    private fun maybeShowForceUpdateDialog() {
        val config = RemoteConfigUtils.getForceUpdateConfig() ?: return
        if (config.storeLink.isBlank()) return
        val needsUpdate = BuildConfig.VERSION_CODE < config.minVersionCode
        if (needsUpdate || config.force) {
            cachedForceUpdateConfig = config
            forceUpdateDialog.show(config)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::consentHandler.isInitialized) {
            consentHandler.clear()
        }
        delayRunnable?.let {
            delayHandler.removeCallbacks(it)
        }
        forceUpdateDialog.dismiss()
    }

    companion object {
        const val EXTRA_TAB = "extra_tab"
        const val TAB_HOME = "home"
        const val TAB_CHANNELS = "channels"
        const val TAB_SETTING = "setting"

        private const val TAG_HOME = "home_fragment"
        private const val TAG_CHANNELS = "channel_fragment"
        private const val TAG_SETTING = "setting_fragment"
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}

object HomeHostUiState : UiState

private enum class HomeTab {
    HOME,
    CHANNELS,
    SETTING,
}
