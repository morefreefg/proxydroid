package me.bwelco.proxy.config

import me.bwelco.proxy.proxy.DirectProxy
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.proxy.RejectProxy

class ProxyConfig(config: Config) : Config by config {

    private val proxylist: Map<String, Proxy> = config.proxyList().toMutableMap().apply {
        put("DIRECT", DirectProxy())
        put("REJECT", RejectProxy())
    }

    override fun proxyList(): Map<String, Proxy> = proxylist
}