package com.youzan.mobile.socks

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5.*
import java.net.Socket
import java.sql.ClientInfoStatus

class Socks5CommandRequestHandler(val bossGroup: EventLoopGroup,
                                  val onConnect: (Socket) -> Unit): SimpleChannelInboundHandler<DefaultSocks5CommandRequest>() {

    override fun channelRead0(socksClientContext: ChannelHandlerContext?, msg: DefaultSocks5CommandRequest?) {
        if (socksClientContext == null || msg == null) return
        if (msg.type() == Socks5CommandType.CONNECT) {
            val bootstrap = Bootstrap()
            bootstrap.group(bossGroup)
                    .channel(CustomNioSocketChannel::class.java)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            onConnect((ch as CustomNioSocketChannel).rawSocket)
                        }
                    })

            val connectFuture = bootstrap.connect(msg.dstAddr(), msg.dstPort())
            connectFuture.addListener {
                if (connectFuture.isSuccess) {
                    val serverChannel = connectFuture.channel()
                    val clientChannel = socksClientContext.channel()

                    connect2Server(serverChannel, clientChannel)

                    val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4)
                    clientChannel.writeAndFlush(commandResponse)
                } else {
                    val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4)
                    socksClientContext.writeAndFlush(commandResponse)
                }
            }

        } else {
            socksClientContext.fireChannelRead(msg)
        }
    }

    private fun connect2ServerWithSocksProtocol(clientChannel: Channel, serverChannel: Channel) {
        clientChannel.pipeline().addLast()
    }

    private fun connect2Server(clientChannel: Channel, serverChannel: Channel) {
        clientChannel.pipeline().addLast(Client2DestHandler(serverChannel))
        serverChannel.pipeline().addLast(Dest2ClientHandler(clientChannel))
    }

    private class Dest2ClientHandler(private val socksClientChannel: Channel) : ChannelInboundHandlerAdapter() {

        override fun channelRead(ctx: ChannelHandlerContext, destMsg: Any) {
            socksClientChannel.writeAndFlush(destMsg)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            socksClientChannel.close()
        }
    }

    private class Client2DestHandler(private val destChannel: Channel) : ChannelInboundHandlerAdapter() {

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            destChannel.writeAndFlush(msg)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            destChannel.close()
        }
    }
}