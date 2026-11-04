package com.anrstudio.template.ui.bases

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.anrstudio.template.R
import com.anrstudio.template.app.AppConstants
import com.anrstudio.template.data.pref.AppSharedPref
import com.anrstudio.template.data.pref.AppSharedPreferencesApp
import com.anrstudio.template.utils.ANRTrackingHelper
import com.anrstudio.template.utils.Routes
import java.util.Locale
import javax.inject.Inject


data class StatusBarConfig(
    val isVisible: Boolean = true,
    val applyPadding: Boolean = true,
    val barColor: Int = Color.TRANSPARENT,
    val lightIcons: Boolean = true
)

data class NavigationBarConfig(
    val isVisible: Boolean = false,
    val applyPadding: Boolean = false,
    val barColor: Int = Color.TRANSPARENT,
    val lightIcons: Boolean = true
)

data class ImeConfig(
    val hideOnOutsideTouch: Boolean = false,
    val resizeWhenKeyboardVisible: Boolean = false
)

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {

    @Inject
    lateinit var appSharedPref: AppSharedPref


    lateinit var mBinding: VB


    private var loadingView: View? = null


    protected open val statusBarConfig = StatusBarConfig()

    protected open val navigationBarConfig = NavigationBarConfig()

    protected open val imeConfig = ImeConfig()
    private var isKeyboardVisible = false


    override fun attachBaseContext(base: Context) {
        val currentLanguage = AppSharedPreferencesApp(base).languageCode
        val currentLocale = Locale.Builder().setLanguage(currentLanguage).build()
        super.attachBaseContext(updateResourcesLocale(base, currentLocale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logScreenTracking()

        requestWindow()

        val layoutView = getLayoutActivity()
        mBinding = DataBindingUtil.setContentView(this, layoutView)
        mBinding.lifecycleOwner = this

        setupSystemBars()
        registerBackPressedCallback()
        initLoadingView()

        initViews()
        onResizeViews()
        onClickViews()
        observeData()
    }


    protected abstract fun getLayoutActivity(): Int

    protected open fun getRootView(): View = mBinding.root
    protected open fun onActivityBackPressed() {
        finish()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
//        } else {
//            @Suppress("DEPRECATION")
//            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
//        }
    }


    protected open fun requestWindow() {}
    protected open fun initViews() {}
    protected open fun onResizeViews() {}
    protected open fun onClickViews() {}
    protected open fun observeData() {}


    open fun onKeyboardVisible() {}
    open fun onKeyboardGone() {}


    protected fun showLoading() {
        if (loadingView == null) {
            initLoadingView()
        }
        loadingView?.isVisible = true
    }

    protected fun hideLoading() {
        loadingView?.isVisible = false
    }


    private fun setupSystemBars() {
        enableEdgeToEdge()

        val rootView = getRootView()
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val controller = WindowInsetsControllerCompat(window, view)

            val shouldShowStatusBar = statusBarConfig.isVisible
            val shouldShowNavigationBar = navigationBarConfig.isVisible
            val resizeLayoutWhenKeyboardVisible = imeConfig.resizeWhenKeyboardVisible

            val statusType = WindowInsetsCompat.Type.statusBars()
            val navigationType = WindowInsetsCompat.Type.navigationBars()
            val imeType = WindowInsetsCompat.Type.ime()


            // light system bars
            val lightStatusBar = statusBarConfig.lightIcons
            controller.isAppearanceLightStatusBars = lightStatusBar
            val lightNavigationBar = navigationBarConfig.lightIcons
            controller.isAppearanceLightNavigationBars = lightNavigationBar


            // apply system bars color
            val statusBarColor = statusBarConfig.barColor
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor
            val navigationBarColor = navigationBarConfig.barColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = navigationBarColor


            // show/hide system bars
            val hiddenStatusType = if (!shouldShowStatusBar) statusType else 0
            val hiddenNavigationType = if (!shouldShowNavigationBar) navigationType else 0
            val hiddenMaskedTypes = hiddenStatusType or hiddenNavigationType
            if (hiddenMaskedTypes != 0) {
                controller.hide(hiddenMaskedTypes)
                controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }


            // handle keyboard shown events
            val isKeyboardVisible = insets.isVisible(imeType)
            if (isKeyboardVisible != this.isKeyboardVisible) {
                this.isKeyboardVisible = isKeyboardVisible
                if (isKeyboardVisible) onKeyboardVisible() else onKeyboardGone()
            }


            // apply system bars padding
            val applyStatusBarPadding = statusBarConfig.applyPadding
            val applyNavigationBarPadding = navigationBarConfig.applyPadding
            var maskedTypes = 0
            if (applyStatusBarPadding) maskedTypes = maskedTypes or statusType
            if (applyNavigationBarPadding) maskedTypes = maskedTypes or navigationType
            if (resizeLayoutWhenKeyboardVisible) maskedTypes = maskedTypes or imeType
            val maskedInsets = insets.getInsets(maskedTypes)

            val paddingTop = maskedInsets.top
            val paddingBottom =
                if (!resizeLayoutWhenKeyboardVisible && !shouldShowNavigationBar && !applyNavigationBarPadding) 0
                else maskedInsets.bottom

            if ((paddingTop != 0 && applyStatusBarPadding) || !applyStatusBarPadding) {
                view.updatePadding(top = paddingTop)
            }
            if ((paddingBottom != 0 && applyNavigationBarPadding) || !applyNavigationBarPadding) {
                view.updatePadding(bottom = paddingBottom)
            }

            insets
        }
    }

    private fun initLoadingView() {
        val parent: ViewGroup = findViewById(android.R.id.content)
        if (loadingView == null) {
            loadingView = LayoutInflater.from(this).inflate(R.layout.view_loading, parent, false)
            loadingView?.isVisible = false
            parent.addView(loadingView)
        }
    }

    private fun registerBackPressedCallback() {
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onActivityBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun logScreenTracking() {
        ANRTrackingHelper.addScreenTrack(this::class.java.simpleName)
        if (intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM) != null) {
            Routes.addTrackingMoveScreen(
                intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM).toString(),
                this::class.java.simpleName
            )
        }
    }

    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}