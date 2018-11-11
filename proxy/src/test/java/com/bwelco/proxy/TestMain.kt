package com.bwelco.proxy

import me.bwelco.proxy.ProxyServer
import me.bwelco.proxy.rule.CustomRules
import org.junit.Test

public class TestMain {

    @Test
    fun test() {
        ProxyServer.startUp(rules = CustomRules())
    }
}