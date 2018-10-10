package me.bwelco.proxy.proxy

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
import me.bwelco.proxy.action.DirectAction
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.http.ProtocolSelectHandler
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.upstream.DirectUpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandler
import me.bwelco.proxy.util.closeOnFlush
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

@ChannelHandler.Sharable
class UpstreamMatchHandler :
        SimpleChannelInboundHandler<SocksMessage>(), KoinComponent {

    private val proxyConfig: ProxyRules by inject()

    private fun matchUpstream(message: Socks5CommandRequest, promise: Promise<Channel>): UpstreamHandler {
        return proxyConfig.proxylist[proxyConfig.proxyMatcher(message.dstAddr())]
                ?.createProxyHandler(message, promise)
                ?: DirectUpstreamHandler(message, promise)
    }

    override fun channelRead0(clientCtx: ChannelHandlerContext, message: SocksMessage) {

        when (message) {
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
                        clientCtx.channel().closeOnFlush()
                    }
                }

                clientCtx.pipeline().remove(this)

                clientCtx.pipeline().addLast(matchUpstream(message, commandResponsePromise.addListener {
                    if (it.isSuccess) {
                        val remoteChannel = it.now as Channel

                        clientCtx.channel()
                                .pipeline()
                                .addLast(ProtocolSelectHandler(
                                        remoteChannel = remoteChannel,
                                        socks5Request = message,
                                        followUpAction = proxyConfig.proxylist
                                                [proxyConfig.proxyMatcher(message.dstAddr())]?.followUpAction()
                                                ?: DirectAction()))
                    }
                }))

                clientCtx.pipeline().fireChannelRegistered()
                clientCtx.pipeline().fireChannelActive()
            }

            is Socks4CommandRequest -> clientCtx.close()
            else -> clientCtx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.channel().closeOnFlush()
    }

}