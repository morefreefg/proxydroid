package me.bwelco.proxy.proxy

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class ProxySelectorHandler : ByteToMessageDecoder() {

    val SSL_RT_HANDSHAKE = 0x16

    val SSL3_VERSION = 0x0300
    val TLS1_1_VERSION = 0x0301
    val TLS1_2_VERSION = 0x0302
    val TLS1_3_VERSION = 0x0303

    val SUPPORTED_TLS_VERSIONS = listOf(SSL3_VERSION, TLS1_1_VERSION, TLS1_2_VERSION, TLS1_3_VERSION)


    override fun decode(ctx: ChannelHandlerContext, inBuff: ByteBuf, out: MutableList<Any>) {
        if (inBuff.readableBytes() < 3) return

        val type = inBuff.getByte(0)
        val versionHigh = inBuff.getByte(1)
        val versionLow = inBuff.getByte(2)

        val version = (versionHigh.toInt() shl 2) + versionLow.toInt()

        // is TLS
        if (type.toInt() == SSL_RT_HANDSHAKE && SUPPORTED_TLS_VERSIONS.contains(version)) {

        }
    }

}