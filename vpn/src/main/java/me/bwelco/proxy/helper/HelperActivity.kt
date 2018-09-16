package me.bwelco.proxy.helper

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.security.KeyChain
import android.support.v4.content.ContextCompat
import me.bwelco.proxy.ProxyService

class HelperActivity : Activity() {

    companion object {
        const val COMMAND_INSTALL_CA = "command_install_ca"
        const val COMMAND_START_PROXY = "command_start_proxy"
        const val COMMAND_STOP_PROXY = "command_stop_proxy"

        const val COMMAND = "command"

        const val PROXY_CLASS_EXTRA = "proxy_class"
    }

    private val REQUEST_INSTALL_CA = 1
    private val REQUEST_START_PROXY = 2

    private fun certBytes(): ByteArray {
        val classLoader = Thread.currentThread().contextClassLoader
        val caCert = classLoader.getResourceAsStream("ca.crt")

        val length = caCert.available()
        val byteArray = ByteArray(length)
        caCert.read(byteArray, 0, length)
        return byteArray
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val command = intent.getStringExtra(COMMAND)
        when (command) {
            COMMAND_INSTALL_CA -> {
                val intent = KeyChain.createInstallIntent()
                intent.putExtra("CERT", certBytes())
                intent.putExtra("name", "proxdroid ca")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityForResult(intent, REQUEST_INSTALL_CA)
            }

            COMMAND_START_PROXY -> {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startProxy()
                } else {
                    startActivityForResult(intent, REQUEST_START_PROXY)
                }
            }

            COMMAND_STOP_PROXY -> {
                stopProxy()
            }
        }
    }

    private fun startProxy() {
        try {
            val clazz = intent.getSerializableExtra(PROXY_CLASS_EXTRA) as Class<out ProxyService>? ?: return
            val intent = Intent(this, clazz)
            ContextCompat.startForegroundService(this, intent)
            VpnHelper.startProxyCallback?.invoke(true, null)
        } catch (e: Exception) {
            VpnHelper.startProxyCallback?.invoke(false, e)
        } finally {
            VpnHelper.startProxyCallback = null
            finish()
        }
    }


    private fun stopProxy() {
        try {
            val intent = Intent(ProxyService.STOP_ACTION)
            sendBroadcast(intent)
            VpnHelper.stopProxyCallback?.invoke(true, null)
        } catch (e: Exception) {
            VpnHelper.stopProxyCallback?.invoke(false, e)
        } finally {
            VpnHelper.stopProxyCallback = null
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_INSTALL_CA -> {
                    VpnHelper.installCaCallBack?.invoke(true, null)
                    VpnHelper.installCaCallBack = null
                    finish()
                }

                REQUEST_START_PROXY -> {
                    startProxy()
                }
            }
        } else {
            when(requestCode)  {
                REQUEST_INSTALL_CA -> {
                    VpnHelper.installCaCallBack?.invoke(false, null)
                    VpnHelper.installCaCallBack = null
                }

                REQUEST_START_PROXY -> {
                    VpnHelper.startProxyCallback?.invoke(false, null)
                    VpnHelper.startProxyCallback = null
                }
            }
            finish()
        }
    }
}