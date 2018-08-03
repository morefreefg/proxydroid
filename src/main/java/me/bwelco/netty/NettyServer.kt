package me.bwelco.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.CharsetUtil
import java.net.InetSocketAddress
import sun.java2d.opengl.OGLRenderQueue.sync


class NettyServer(val port: Int) {

    class HelloServerHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
            val inBuffer = msg as ByteBuf
            val received = inBuffer.toString(CharsetUtil.UTF_8);
            println("Server received: " + received)
            ctx?.writeAndFlush(Unpooled.copiedBuffer("Hello " + received, CharsetUtil.UTF_8));
        }

        override fun channelReadComplete(ctx: ChannelHandlerContext?) {
//            ctx?.writeAndFlush(Unpooled.EMPTY_BUFFER)
//                    ?.addListener(ChannelFutureListener.CLOSE);
            println("read complete")
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
            cause?.printStackTrace();
            ctx?.close();
        }
    }

    fun startServer() {

        val group = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
                    .channel(NioServerSocketChannel::class.java)
                    .localAddress(InetSocketAddress(port))
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel?) {
                            ch?.pipeline()?.addLast(HelloServerHandler())
                        }
                    })

            val channelFuture = serverBootstrap.bind().sync()
            channelFuture.channel().closeFuture().sync()
        } catch(e: Exception){
            e.printStackTrace();
        } finally {
            group.shutdownGracefully().sync();
        }

    }

}