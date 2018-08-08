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
import java.net.Socket

@ChannelHandler.Sharable
class SocksServerConnectHandler(val connectListener: (Socket) -> Unit): SimpleChannelInboundHandler<SocksMessage>() {

    val bootstrap: Bootstrap by lazy { Bootstrap() }

    override fun channelRead0(clientCtx: ChannelHandlerContext, message: SocksMessage) {
        val inboundChannel = clientCtx.channel()

        when(message) {
            is Socks5CommandRequest -> {
                val request = message

                bootstrap.group(inboundChannel.eventLoop())
                        .channel(CustomNioSocketChannel::class.java)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(object : ChannelInboundHandlerAdapter() {
                            override fun channelActive(serverCtx: ChannelHandlerContext) {

                                val connectResultHandler = this

                                val outboundChannel = serverCtx.channel()

                                val responseFuture = inboundChannel.writeAndFlush(DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.SUCCESS,
                                        request.dstAddrType(),
                                        request.dstAddr(),
                                        request.dstPort()))

                                responseFuture.addListener(object : ChannelFutureListener {
                                    override fun operationComplete(future: ChannelFuture) {
                                        if (future.isSuccess) {
                                            outboundChannel.pipeline().remove(connectResultHandler)
                                            outboundChannel.pipeline().addLast(DirectClientHandler(inboundChannel))
                                            outboundChannel.pipeline().fireChannelRegistered()
                                            outboundChannel.pipeline().fireChannelActive()
                                        } else {
                                            exceptionCaught(serverCtx, future.cause())
                                        }
                                    }
                                })
                            }

                            override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
                                inboundChannel.writeAndFlush(
                                        DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
                                SocksServerUtils.closeOnFlush(inboundChannel)
                            }
                        })

                bootstrap.connect(request.dstAddr(), request.dstPort()).addListener {
                    if (it?.isSuccess?:false) {
                        inboundChannel.pipeline().remove(this)
                    }
                }
            }

            is Socks4CommandRequest -> clientCtx.close()
            else -> clientCtx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        SocksServerUtils.closeOnFlush(ctx.channel())
    }

}