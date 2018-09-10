package me.bwelco.demo

import me.bwelco.proxy.rule.Rules
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.proxy.Socks5Proxy
import java.net.Inet4Address

class CustomRules: Rules {

    override val mitmConfig: HttpInterceptorMatcher? = null

    override val proxylist =
            mutableMapOf<String, Proxy>("socks" to Socks5Proxy(Inet4Address.getByName("172.17.13.68"), 6153))

    override fun proxyMatcher(host: String): String {
        return "DIRECT"
    }
}