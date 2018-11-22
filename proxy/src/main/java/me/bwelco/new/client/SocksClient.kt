package me.bwelco.new.client

import io.netty.channel.ChannelHandler
import me.bwelco.new.init.SocksProxyInitHandler

class SocksClient: ProxyClient() {
    override val initHandler: ChannelHandler by lazy { SocksProxyInitHandler() }
    override val port: Int = 1080
}