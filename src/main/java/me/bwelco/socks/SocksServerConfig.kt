package me.bwelco.socks

import java.net.InetAddress

data class SocksServerConfig(val ipAddr: InetAddress,
                             val port: Int)