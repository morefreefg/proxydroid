package me.bwelco.demo

import me.bwelco.proxy.ProxyService
import me.bwelco.proxy.rule.Rules

class CustomProxyService : ProxyService() {
    override val rules: Rules = CustomRules()

}