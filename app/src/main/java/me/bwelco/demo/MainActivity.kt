package me.bwelco.demo

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Button
import me.bwelco.proxy.ProxyService
import me.bwelco.proxy.helper.VpnHelper


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start_proxy).setOnClickListener {
            VpnHelper.startProxy(this, CustomProxyService::class.java)
        }

        findViewById<Button>(R.id.stop_proxy).setOnClickListener {
            VpnHelper.stopProxy(this, CustomProxyService::class.java)
        }

        findViewById<Button>(R.id.install_cert).setOnClickListener {
            VpnHelper.installCA(this)
        }
    }
}
