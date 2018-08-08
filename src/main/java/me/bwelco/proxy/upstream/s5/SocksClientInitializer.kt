package me.bwelco.proxy.upstream.s5

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.v5.*
import me.bwelco.proxy.handler.RelayHandler
import me.bwelco.proxy.util.addFutureListener

/**
 * downStreamChannel is the channel where to communicate. such as a Socks5ClientChannel
 */
class SocksClientInitializer(val downStreamChannel: Channel,
                             val request: Socks5CommandRequest): ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        val inBoundChannel = ctx.channel()
        inBoundChannel.pipeline().addLast(Socks5ClientEncoder.DEFAULT)

        inBoundChannel.writeAndFlush(DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH)).addFutureListener { channelFuture ->
            channelFuture.channel().pipeline().addLast(Socks5InitialResponseDecoder())
            channelFuture.channel().pipeline().addLast(Socks5InitialResponseHandler())
        }
    }

    inner class Socks5InitialResponseHandler: SimpleChannelInboundHandler<DefaultSocks5InitialResponse>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5InitialResponse) {
            if (msg.decoderResult().isSuccess) {
                ctx.writeAndFlush(request)
                ctx.pipeline().remove(this)
                ctx.pipeline().addLast(Socks5CommandResponseDecoder())
                ctx.pipeline().addLast(SocksCommandResponseHandler())
            }
        }
    }

    inner class SocksCommandResponseHandler: SimpleChannelInboundHandler<DefaultSocks5CommandResponse>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandResponse) {
            if (msg.decoderResult().isSuccess) {
                val upstreamChannel = ctx.channel()
                upstreamChannel.pipeline().remove(this)

                upstreamChannel.pipeline().addLast(RelayHandler(downStreamChannel))
                downStreamChannel.pipeline().addLast(RelayHandler(upstreamChannel))

                upstreamChannel.pipeline().fireChannelActive()
                downStreamChannel.pipeline().fireChannelActive()
            }
        }

    }
}