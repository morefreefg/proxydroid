package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.DirectAction
import me.bwelco.proxy.http.ProtocolSelectHandler
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.util.closeOnFlush
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

@ChannelHandler.Sharable
class UpstreamMatchHandler :
        SimpleChannelInboundHandler<SocksMessage>(), KoinComponent {

    override fun channelRead0(clientCtx: ChannelHandlerContext, request: SocksMessage) {

        when (request) {
            is Socks5CommandRequest -> {

                clientCtx.writeAndFlush(DefaultSocks5CommandResponse(
                        Socks5CommandStatus.SUCCESS,
                        request.dstAddrType(),
                        request.dstAddr(),
                        request.dstPort())).addListener {
                    clientCtx.pipeline().remove(this)
                    clientCtx.pipeline().addLast(ProtocolSelectHandler(request))
                }

//                val upstreamPromise: Promise<Channel> = clientCtx.executor().newPromise()
//
//                upstreamPromise.addListener { future ->
//                    if (future.isSuccess) {
//                        clientCtx.writeAndFlush(DefaultSocks5CommandResponse(
//                                Socks5CommandStatus.SUCCESS,
//                                request.dstAddrType(),
//                                request.dstAddr(),
//                                request.dstPort()))
//                    } else {
//                        clientCtx.channel().writeAndFlush(
//                                DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
//                        clientCtx.channel().closeOnFlush()
//                    }
//                }
//
//                clientCtx.pipeline().remove(this)
//
//                val dstAddrType = request.dstAddrType()
//
//                val socks5UrlTypePromise = upstreamPromise.addListener {
//                    if (it.isSuccess) {
//                        val remoteChannel = it.now as Channel
//
//                        clientCtx.channel()
//                                .pipeline()
//                                .addLast(ProtocolSelectHandler(
//                                        remoteChannel = remoteChannel,
//                                        socks5Request = request,
//                                        followUpAction = proxyConfig.proxylist
//                                                [proxyConfig.proxyMatcher(request.dstAddr())]?.followUpAction()
//                                                ?: DirectAction()))
//                    }
//                }
//
//                val upstreamHandler = proxyConfig
//                        .proxylist[proxyConfig.proxyMatcher(request.dstAddr())]?.createProxyHandler(request, upstreamPromise)
//                        ?: when {
//                            dstAddrType.equals(Socks5AddressType.IPv4)
//                                    || dstAddrType.equals(Socks5AddressType.IPv6) -> RuleUpstreamHandler(request, upstreamPromise)
//                            else -> DirectProxy().createProxyHandler(request, socks5UrlTypePromise)
//                        }
//
//                clientCtx.pipeline().addLast(upstreamHandler)
//                clientCtx.pipeline().fireChannelRegistered()
//                clientCtx.pipeline().fireChannelActive()
            }

            is Socks4CommandRequest -> {
                clientCtx.writeAndFlush(DefaultSocks4CommandResponse(
                        Socks4CommandStatus.SUCCESS,
                        request.dstAddr(),
                        request.dstPort()
                )).addListener {
                    clientCtx.pipeline().remove(this)
                    clientCtx.pipeline().addLast(ProtocolSelectHandler(request))
                }

            }
            else -> clientCtx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.channel().closeOnFlush()
    }

}