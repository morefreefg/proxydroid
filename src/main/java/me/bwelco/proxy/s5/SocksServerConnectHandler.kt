package me.bwelco.proxy.s5

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.handler.DirectClientHandler
import me.bwelco.proxy.handler.SocksUpstreamClientHandler
import me.bwelco.proxy.upstream.s5.SocksClientInitializer
import java.net.Socket

@ChannelHandler.Sharable
class SocksServerConnectHandler(val connectListener: (Socket) -> Unit): SimpleChannelInboundHandler<SocksMessage>() {

    val bootstrap: Bootstrap by lazy { Bootstrap() }

    override fun channelRead0(clientCtx: ChannelHandlerContext, message: SocksMessage) {
        val inboundChannel = clientCtx.channel()

        when(message) {
            is Socks5CommandRequest -> {
//                clientCtx.pipeline().addLast(DirectClientHandler(message))
                clientCtx.pipeline().addLast(SocksUpstreamClientHandler(message))

                clientCtx.pipeline().fireChannelRegistered()
                clientCtx.pipeline().fireChannelActive()
            }

            is Socks4CommandRequest -> clientCtx.close()
            else -> clientCtx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        SocksServerUtils.closeOnFlush(ctx.channel())
    }

}