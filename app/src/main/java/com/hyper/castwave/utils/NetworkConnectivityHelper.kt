package com.hyper.castwave.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkConnectivityHelper {

    fun hasNetworkWithInternetCapability(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun verifyInternetAccess(context: Context, onResult: (Boolean) -> Unit) {
        onResult(hasNetworkWithInternetCapability(context))
    }
}
