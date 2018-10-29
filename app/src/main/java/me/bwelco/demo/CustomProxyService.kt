package me.bwelco.demo

import android.app.Activity
import me.bwelco.proxy.ProxyService
import me.bwelco.proxy.rule.Rules

class CustomProxyService : ProxyService() {
    override val rules: Rules by lazy {
        val sp = this.getSharedPreferences("sp", Activity.MODE_PRIVATE)
        val userName = sp.getString("username", "")
        val passwd = sp.getString("passwd", "")
        val ip = sp.getString("ip", "")
        val port = sp.getInt("port", -1)

        CustomRules(userName, passwd, ip, port)
    }
}