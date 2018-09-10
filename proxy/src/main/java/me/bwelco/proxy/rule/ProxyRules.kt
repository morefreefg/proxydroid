package me.bwelco.proxy.rule

import me.bwelco.proxy.proxy.DirectProxy
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.proxy.RejectProxy

class ProxyRules(rules: Rules) : Rules by rules {

    override val proxylist: Map<String, Proxy> = rules.proxylist.toMutableMap().apply {
        put("DIRECT", DirectProxy())
        put("REJECT", RejectProxy())
    }

}