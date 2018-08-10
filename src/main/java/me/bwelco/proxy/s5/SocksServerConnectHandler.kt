package me.bwelco.proxy.s5

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.proxy.DirectClientProxy
import me.bwelco.proxy.proxy.HttpUpstreamProxy
import java.net.Socket

@ChannelHandler.Sharable
class SocksServerConnectHandler(val connectListener: (Socket) -> Unit): SimpleChannelInboundHandler<SocksMessage>() {

    override fun channelRead0(clientCtx: ChannelHandlerContext, message: SocksMessage) {

        when(message) {
            is Socks5CommandRequest -> {

                val commandResponsePromise: Promise<Channel> = clientCtx.executor().newPromise()
                commandResponsePromise.addListener { future ->
                    if (future.isSuccess) {
                        clientCtx.writeAndFlush(DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                message.dstAddrType(),
                                message.dstAddr(),
                                message.dstPort()))
                    } else {
                        clientCtx.channel().writeAndFlush(
                                DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, message.dstAddrType()))
                        SocksServerUtils.closeOnFlush(clientCtx.channel())
                    }
                }

                clientCtx.pipeline().remove(this)
//                clientCtx.pipeline().addLast(DirectClientProxy(message, commandResponsePromise))
                clientCtx.pipeline().addLast(HttpUpstreamProxy(message, commandResponsePromise))

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