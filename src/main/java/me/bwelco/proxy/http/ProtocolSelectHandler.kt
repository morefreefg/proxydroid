package me.bwelco.proxy.http

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class ProtocolSelectHandler(val remoteChannel: Channel) : ReplayingDecoder<ProtocolSelectHandler.State>(State.INIT) {

    companion object {
        val SSL_RT_HANDSHAKE = 0x16

        val SSL3_VERSION = 0x0300
        val TLS1_1_VERSION = 0x0301
        val TLS1_2_VERSION = 0x0302
        val TLS1_3_VERSION = 0x0303

        val SUPPORTED_TLS_VERSIONS = listOf(SSL3_VERSION, TLS1_1_VERSION, TLS1_2_VERSION, TLS1_3_VERSION)
    }

    enum class State {
        INIT,
        SUCCESS
    }

    override fun decode(ctx: ChannelHandlerContext, inBuff: ByteBuf, out: MutableList<Any>) {

        when (state()) {
            State.INIT -> {
                if (inBuff.readableBytes() < 3) return

                val type = inBuff.getByte(0)
                val versionHigh = inBuff.getByte(1)
                val versionLow = inBuff.getByte(2)

                // versionHigh << 8 + versionLow
                val version = (versionHigh.toInt() shl 8) + versionLow.toInt()

                // is TLS
                if (type.toInt() == SSL_RT_HANDSHAKE && SUPPORTED_TLS_VERSIONS.contains(version)) {
                    ctx.pipeline().addLast(SniHandler(remoteChannel))
                } else {

                }

                checkpoint(State.SUCCESS)

                val readableBytes = actualReadableBytes()
                if (readableBytes > 0) {
                    out.add(inBuff.readRetainedSlice(readableBytes))
                }
            }

            State.SUCCESS -> {
                val readableBytes = actualReadableBytes()
                if (readableBytes > 0) {
                    out.add(inBuff.readRetainedSlice(readableBytes))
                }
            }
        }
    }

}