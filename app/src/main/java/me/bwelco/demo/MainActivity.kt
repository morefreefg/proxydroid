package me.bwelco.demo

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Button
import me.bwelco.proxy.ProxyService
import android.security.KeyChain
import me.bwelco.proxy.tls.CertUtil


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CONNECT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.start_proxy).setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startProxy()
            } else {
                startActivityForResult(intent, REQUEST_CONNECT)
            }
        }

        findViewById<Button>(R.id.stop_proxy).setOnClickListener {
            stopProxy()
        }

        findViewById<Button>(R.id.install_cert).setOnClickListener {
            installCertificate()
        }
    }


    fun installCertificate() {
        val intent = KeyChain.createInstallIntent()
        intent.putExtra("CERT", certBytes())
        intent.putExtra("name", "this is name")
        startActivityForResult(intent, 1)
    }


    fun certBytes(): ByteArray {
        val classLoader = Thread.currentThread().contextClassLoader
        val caCert = classLoader.getResourceAsStream("ca.crt")

        val length = caCert.available()
        val byteArray = ByteArray(length)
        caCert.read(byteArray, 0, length)
        return byteArray
    }


    fun startProxy() {
        val intent = Intent(this, ProxyService::class.java)
        intent.putExtra(ProxyService.COMMAND, ProxyService.START_COMMAND)
        ContextCompat.startForegroundService(this, intent)
    }


    fun stopProxy() {
        val intent = Intent(this, ProxyService::class.java)
        intent.putExtra(ProxyService.COMMAND, ProxyService.STOP_COMMAND)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CONNECT) {
            startProxy()
        }
    }
}
