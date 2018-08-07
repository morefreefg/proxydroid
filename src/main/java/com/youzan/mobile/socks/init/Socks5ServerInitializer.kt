package com.youzan.mobile.socks.init

import com.youzan.mobile.socks.Socks5CommandRequestHandler
import com.youzan.mobile.socks.Socks5InitialRequestHandler
import com.youzan.mobile.socks.SocksServerConfig
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder
import java.net.Socket

class Socks5ServerInitializer(private val bossGroup: NioEventLoopGroup,
                              private val onConnect: (Socket) -> Unit = {},
                              private val upstreamSocksServer: SocksServerConfig? = null): ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel?) {
        if (ch == null) return

        //Socks5MessagByteBuf
        ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT)
        //sock5 init
        ch.pipeline().addLast(Socks5InitialRequestDecoder())
        //sock5 init
        ch.pipeline().addLast(Socks5InitialRequestHandler())
        //socks connection
        ch.pipeline().addLast(Socks5CommandRequestDecoder())
        //Socks connection
        ch.pipeline().addLast(Socks5CommandRequestHandler(bossGroup, onConnect))
    }

}