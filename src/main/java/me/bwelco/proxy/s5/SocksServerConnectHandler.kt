package me.bwelco.proxy.s5

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import me.bwelco.proxy.CustomNioSocketChannel
import java.net.Socket

@ChannelHandler.Sharable
class SocksServerConnectHandler(val connectListener: (Socket) -> Unit): SimpleChannelInboundHandler<SocksMessage>() {

    val bootstrap: Bootstrap by lazy { Bootstrap() }

    override fun channelRead0(ctx: ChannelHandlerContext, message: SocksMessage) {
        val inboundChannel = ctx.channel()

        when(message) {
            is Socks5CommandRequest -> {
                val request = message

                bootstrap.group(inboundChannel.eventLoop())
                        .channel(CustomNioSocketChannel::class.java)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(DirectClientHandler(inboundChannel, request))

                bootstrap.connect(request.dstAddr(), request.dstPort()).addListener {
                    if (it?.isSuccess?:false) {
                        inboundChannel.pipeline().remove(this)
                    }
                }
            }

            is Socks4CommandRequest -> ctx.close()
            else -> ctx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        SocksServerUtils.closeOnFlush(ctx.channel())
    }

}