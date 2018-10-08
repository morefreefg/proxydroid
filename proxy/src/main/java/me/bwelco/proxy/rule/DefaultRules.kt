package me.bwelco.proxy.rule

import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.Proxy

class DefaultRules : Rules {

    override val proxylist: Map<String, Proxy> = mapOf()

    override fun proxyMatcher(host: String): String? = "DIRECT"

    override val mitmConfig = object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? = null
    }

}