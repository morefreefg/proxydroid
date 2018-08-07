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

    val b = Bootstrap()

    override fun channelRead0(ctx: ChannelHandlerContext?, message: SocksMessage?) {
        if (message == null || ctx == null) return

        when(message) {
            is Socks4CommandRequest -> {
                val request = message
                val promise = ctx.executor().newPromise<Channel>()
                promise.addListener { future ->
                    val outboundChannel = future.now as Channel
                    if (future.isSuccess) {
                        val responseFuture = ctx.channel().writeAndFlush(
                                DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS))

                        responseFuture.addListener {
                            ctx.pipeline().remove(this@SocksServerConnectHandler)
                            outboundChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                            ctx.pipeline().addLast(RelayHandler(outboundChannel))
                        }
                    } else {
                        ctx.channel().writeAndFlush(
                                DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED))
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }

                val inboundChannel = ctx.channel()
                b.group(inboundChannel.eventLoop())
                        .channel(CustomNioSocketChannel::class.java)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(DirectClientHandler(promise, connectListener))

                b.connect(request.dstAddr(), request.dstPort()).addListener { future ->
                    if (future.isSuccess) {
                        // Connection established use handler provided results
                    } else {
                        // Close the connection if the connection attempt has failed.
                        ctx.channel().writeAndFlush(
                                DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED)
                        )
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }
            }

            is Socks5CommandRequest -> {
                val request = message
                val promise = ctx.executor().newPromise<Channel>()
                promise.addListener { future ->
                    val outboundChannel = future.now as Channel
                    if (future.isSuccess) {
                        val responseFuture = ctx.channel().writeAndFlush(DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                request.dstAddrType(),
                                request.dstAddr(),
                                request.dstPort()))

                        responseFuture.addListener {
                            ctx.pipeline().remove(this@SocksServerConnectHandler)
                            outboundChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                            ctx.pipeline().addLast(RelayHandler(outboundChannel))
                        }
                    } else {
                        ctx.channel().writeAndFlush(DefaultSocks5CommandResponse(
                                Socks5CommandStatus.FAILURE, request.dstAddrType()))
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }

                val inboundChannel = ctx.channel()
                b.group(inboundChannel.eventLoop())
                        .channel(CustomNioSocketChannel::class.java)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(DirectClientHandler(promise, connectListener))

                b.connect(request.dstAddr(), request.dstPort()).addListener { future ->
                    if (future.isSuccess) {
                        // Connection established use handler provided results
                    } else {
                        // Close the connection if the connection attempt has failed.
                        ctx.channel().writeAndFlush(
                                DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }
            }

            else -> ctx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        SocksServerUtils.closeOnFlush(ctx?.channel()!!)
    }

}