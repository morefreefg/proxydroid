package me.bwelco.proxy.rule

import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.Proxy


interface Rules {

    /**
     * all proxies
     * auto add "DIRECT" and "REJTCT"
     */
    val proxylist: Map<String, Proxy>

    /**
     * return proxyname with host
     * null -> "DIRECT"
     */
    fun proxyMatcher(host: String): String?

    /**
     * return null if not enable mitm
     */
    val mitmConfig: HttpInterceptorMatcher?
}