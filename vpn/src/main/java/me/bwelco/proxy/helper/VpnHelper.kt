package me.bwelco.proxy.helper

import android.content.Context
import android.content.Intent
import me.bwelco.proxy.ProxyService

object VpnHelper {

    interface CallBack {
        fun result(result: Boolean, throwable: Throwable?)
    }

    internal var startProxyCallback: ((result: Boolean, throwable: Throwable?) -> Unit)? = null
    fun startProxy(context: Context,
                   proxyClass: Class<out ProxyService>,
                   callBack: (result: Boolean, throwable: Throwable?) -> Unit = { _, _ -> }) {
        if (startProxyCallback != null) return callBack(false, Throwable("starting...."))
        startProxyCallback = callBack

        val intent = Intent(context, HelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(HelperActivity.COMMAND, HelperActivity.COMMAND_START_PROXY)
        intent.putExtra(HelperActivity.PROXY_CLASS_EXTRA, proxyClass)
        context.startActivity(intent)
    }

    internal var stopProxyCallback: ((result: Boolean, throwable: Throwable?) -> Unit)? = null
    fun stopProxy(context: Context,
                  proxyClass: Class<out ProxyService>,
                  callBack: (result: Boolean, throwable: Throwable?) -> Unit = { _, _ -> }) {
        if (stopProxyCallback != null) return callBack(false, Throwable("stopping...."))
        stopProxyCallback = callBack

        val intent = Intent(context, HelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(HelperActivity.COMMAND, HelperActivity.COMMAND_STOP_PROXY)
        intent.putExtra(HelperActivity.PROXY_CLASS_EXTRA, proxyClass)
        context.startActivity(intent)
    }

    internal var installCaCallBack: ((result: Boolean, throwable: Throwable?) -> Unit)? = null

    fun installCA(context: Context,
                  callBack: (result: Boolean, throwable: Throwable?) -> Unit = { _, _ -> }) {
        if (installCaCallBack != null) return callBack(false, Throwable("installing...."))
        installCaCallBack = callBack

        val intent = Intent(context, HelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(HelperActivity.COMMAND, HelperActivity.COMMAND_INSTALL_CA)
        context.startActivity(intent)
    }

}