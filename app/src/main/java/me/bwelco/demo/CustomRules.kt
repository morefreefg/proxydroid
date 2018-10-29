package me.bwelco.demo

import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.Socks5Proxy
import me.bwelco.proxy.rule.Rules
import java.net.Inet4Address

class CustomRules(val socksUsername: String,
                  val socksPasswd: String,
                  val socksIp: String,
                  val socksPort: Int) : Rules {

    override val mitmConfig: HttpInterceptorMatcher? = object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? {
            return null
        }
    }

    override val proxylist =
            mutableMapOf("socks" to Socks5Proxy(Inet4Address.getByName(socksIp), socksPort, socksUsername, socksPasswd))

    override fun proxyMatcher(host: String): String {
//        return when {
//            host.contains("baidu") -> "http"
//            else -> "http"
//        }
        return "socks"
    }
}