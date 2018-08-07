package com.youzan.mobile.socks

import java.net.InetAddress

data class SocksServerConfig(val ipAddr: InetAddress,
                             val port: Int)