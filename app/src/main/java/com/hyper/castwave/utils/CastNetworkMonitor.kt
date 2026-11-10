package com.hyper.castwave.utils
import com.hyper.castwave.utils.ConnectionLiveData
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.hyper.castwave.ui.component.dialog.DialogNoInternet
class CastNetworkMonitor(private val activity: AppCompatActivity) : DefaultLifecycleObserver {
    private var connectionLiveData: ConnectionLiveData? = null
    private var networkObserver: Observer<Boolean>? = null
    private var noInternetDialog: DialogNoInternet? = null
    fun attach() { activity.lifecycle.addObserver(this) }
    override fun onStart(owner: LifecycleOwner) { startWatchingNetworkConnectivity() }
    override fun onResume(owner: LifecycleOwner) { evaluateNetworkOnScreenVisible() }
    override fun onStop(owner: LifecycleOwner) { stopWatchingNetworkConnectivity() }
    private fun startWatchingNetworkConnectivity() {
        if (networkObserver != null) return
        val liveData = ConnectionLiveData(activity.applicationContext).also { connectionLiveData = it }
        val observer = Observer<Boolean> { isConnected ->
            if (isConnected) {
                dismissNoInternetDialog()
            } else {
                evaluateNetworkOnScreenVisible()
            }
        }.also { networkObserver = it }
        liveData.observe(activity, observer)
        evaluateNetworkOnScreenVisible()
    }

    private fun stopWatchingNetworkConnectivity() {
        networkObserver?.let { connectionLiveData?.removeObserver(it) }
        networkObserver = null
        connectionLiveData = null
        dismissNoInternetDialog()
    }

    private fun evaluateNetworkOnScreenVisible() {
        if (activity.isFinishing || activity.isDestroyed) return
        NetworkConnectivityHelper.verifyInternetAccess(activity) { hasInternet ->
            if (activity.isFinishing || activity.isDestroyed) return@verifyInternetAccess
            if (hasInternet) {
                dismissNoInternetDialog()
            } else {
                showNoInternetDialog()
            }
        }
    }

    private fun showNoInternetDialog() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (noInternetDialog?.isShowing == true) return
        noInternetDialog = DialogNoInternet(activity).also { it.show() }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.takeIf { it.isShowing }?.dismiss()
        noInternetDialog = null
    }
}
