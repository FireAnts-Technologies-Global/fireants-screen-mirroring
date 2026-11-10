package com.hyper.castwave.ui.component.iptv

import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.banner_all
import com.hyper.castwave.ads.inter_back
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.hyper.castwave.R
import com.hyper.castwave.app.GlobalApp
import com.hyper.castwave.ui.bases.BaseActivityWithBanner
import com.hyper.castwave.ui.bases.BannerConfig
import com.hyper.castwave.ui.component.iptv.dialog.NoInternetDialog
import com.hyper.castwave.ui.component.iptv.dialog.NoInternetDialog.Companion.TAG_NO_INTERNET_DIALOG
import com.hyper.castwave.utils.ConnectionLiveData
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.ui.bases.ext.openSettingWifi
import javax.inject.Inject

/** IPTV-specific connectivity/back actions on the application's shared ANR base. */
abstract class IptvBaseActivity<VB : ViewDataBinding> : BaseActivityWithBanner<VB>(),
    NoInternetDialog.NoInternetDialogListener {

    @Inject lateinit var iptvSharedPref: com.hyper.castwave.data.iptv.pref.AppSharedPref

    open var isHasShowAddWhenBack = false
    open var isNeedCheckInternet = true
    protected var isOpenSetting = false
    private val internetListener by lazy { ConnectionLiveData(this) }

    override val bannerConfig: BannerConfig
        get() = BannerConfig(AdRemoteConfig.banner_all.copy(
            reloadIntervalSeconds = 20), false)

    override fun shouldShowBanner() = super.shouldShowBanner() && !GlobalApp.isInPIP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNeedCheckInternet) return
        if (!isNetwork()) whenInternetTurnOff()
        internetListener.observe(this) { connected ->
            if (connected) whenInternetTurnOn() else whenInternetTurnOff()
        }
    }

    override fun onActivityBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (isHasShowAddWhenBack) {
                AdsManager.loadAndShowInterstitial(this, AdRemoteConfig.inter_back) { finish() }
            } else finish()
        } else supportFragmentManager.popBackStackImmediate()
    }

    open fun whenInternetTurnOn() { closeNoInternetDialog() }
    open fun whenInternetTurnOff() { showNoInternetDialog() }

    protected fun closeNoInternetDialog() {
        try {
            (supportFragmentManager.findFragmentByTag(TAG_NO_INTERNET_DIALOG) as? NoInternetDialog)?.dismiss()
        } catch (_: Exception) { }
    }

    protected fun showNoInternetDialog() {
        try {
            if (supportFragmentManager.findFragmentByTag(TAG_NO_INTERNET_DIALOG) !is NoInternetDialog) {
                NoInternetDialog.newInstance().show(supportFragmentManager, TAG_NO_INTERNET_DIALOG)
            }
        } catch (_: Exception) { }
    }

    protected fun showErrorDialog(labelBtnConfirm: String = "", isCancelable: Boolean = false) {
        android.widget.Toast.makeText(this, getString(R.string.cast_error_common), android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onOpenSetting() {
        isOpenSetting = true
        openSettingWifi()
    }
}
