package me.bwelco.proxy.config

import me.bwelco.proxy.proxy.Proxy


interface Config {

    fun proxyList(): MutableMap<String, Proxy>

    fun proxyMatcher(host: String): String

}