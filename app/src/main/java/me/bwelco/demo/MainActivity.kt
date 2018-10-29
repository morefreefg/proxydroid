package me.bwelco.demo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import me.bwelco.proxy.helper.VpnHelper


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val username = findViewById<EditText>(R.id.user_name)
        val passwd = findViewById<EditText>(R.id.pass_word)
        val ip = findViewById<EditText>(R.id.ip)
        val port = findViewById<EditText>(R.id.port)

        val sp = this.getSharedPreferences("sp", Activity.MODE_PRIVATE)
        val userNameSP = sp.getString("username", "")
        val passwdSP = sp.getString("passwd", "")
        val ipSP = sp.getString("ip", "")
        val portSP = sp.getInt("port", -1)

        username.setText(userNameSP)
        passwd.setText(passwdSP)
        ip.setText(ipSP)
        port.setText(portSP.toString())


        findViewById<Button>(R.id.start_proxy).setOnClickListener {
            getSharedPreferences("sp", Activity.MODE_PRIVATE)
                    .edit()
                    .putString("username", username.text.toString())
                    .putString("passwd", passwd.text.toString())
                    .putString("ip", ip.text.toString())
                    .putInt("port", port.text.toString().toInt())
                    .apply()

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
