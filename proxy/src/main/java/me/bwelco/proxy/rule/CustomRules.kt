package me.bwelco.proxy.rule

import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.HttpProxy
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.proxy.Socks5Proxy
import me.bwelco.proxy.rule.Rules
import java.net.Inet4Address

class CustomRules : Rules {

    override val mitmConfig: HttpInterceptorMatcher? = object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? {
            return when {
//                host.contains("baidu") -> BaiduHttpInterceptor()
//                host.contains("360") -> BaiduHttpInterceptor()
                else -> null
            }
        }
    }

    override val proxylist =
            mutableMapOf("socks" to Socks5Proxy(Inet4Address.getByName("127.0.0.1"), 9001, "nodejs", "rules!"))

    override fun proxyMatcher(host: String): String {
//        return when {
//            host.contains("baidu") -> "http"
//            else -> "http"
//        }
        return "socks"
    }
}