package me.bwelco.proxy.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

class CorrectCRLFHander : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            val newMessage = msg.replace(Unpooled.buffer(msg.content().capacity() + 2)
                    .writeBytes(msg.content())
                    .writeByte(0x0d)
                    .writeByte(0x0a)
            )
            newMessage.headers().remove("Content-Length")
                    .add("Content-Length", newMessage.content().capacity())

            msg.release()
            ctx.fireChannelRead(newMessage)
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is FullHttpResponse) {
            val newMessage = msg.replace(Unpooled.buffer(msg.content().capacity() + 2)
                    .writeBytes(msg.content())
                    .writeByte(0x0d)
                    .writeByte(0x0a)
            )
            newMessage.headers().remove("Content-Length").add("Content-Length", newMessage.content().capacity())
            msg.release()
            ctx.write(newMessage, promise)
        } else {
            ctx.write(msg, promise)
        }
    }
}