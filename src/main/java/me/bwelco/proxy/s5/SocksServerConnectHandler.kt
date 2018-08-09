package me.bwelco.proxy.s5

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import me.bwelco.proxy.handler.SocksUpstreamClientHandler
import java.net.Socket

@ChannelHandler.Sharable
class SocksServerConnectHandler(val connectListener: (Socket) -> Unit): SimpleChannelInboundHandler<SocksMessage>() {

    override fun channelRead0(clientCtx: ChannelHandlerContext, message: SocksMessage) {

        when(message) {
            is Socks5CommandRequest -> {
                clientCtx.pipeline().remove(this)
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