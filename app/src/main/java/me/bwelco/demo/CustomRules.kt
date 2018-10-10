package me.bwelco.demo

import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.HttpProxy
import me.bwelco.proxy.proxy.Socks5Proxy
import me.bwelco.proxy.rule.Rules
import java.net.Inet4Address

class CustomRules : Rules {

    override val mitmConfig: HttpInterceptorMatcher? = object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? {
            return when {
                host.contains("baidu") -> BaiduHttpInterceptor()
                else -> null
            }
        }
    }

    override val proxylist =
            mutableMapOf("socks" to Socks5Proxy(Inet4Address.getByName("172.17.13.18"), 6153),
                    "http" to HttpProxy(Inet4Address.getByName("172.17.13.68"), 8888))

    override fun proxyMatcher(host: String): String {
//        return when {
//            host.contains("baidu") -> "http"
//            else -> "http"
//        }
        return "socks"
    }
}