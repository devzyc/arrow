package com.zyc.arrow

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/** @author zeng_yong_chang@163.com
 */
object NetworkUtil {

    fun isNetworkAvailable(context: Context) = isAvailable(getConnectivityManager(context).activeNetworkInfo)

    fun isWifiAvailable(context: Context) = isNetworkAvailable(context, ConnectivityManager.TYPE_WIFI)

    fun isMobileAvailable(context: Context) = isNetworkAvailable(context, ConnectivityManager.TYPE_MOBILE)

    private fun isNetworkAvailable(
        context: Context,
        type: Int
    ) = isAvailable(getConnectivityManager(context).getNetworkInfo(type))

    private fun getConnectivityManager(context: Context) = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun isAvailable(ni: NetworkInfo?) = ni != null && ni.isAvailable && ni.isConnected
}