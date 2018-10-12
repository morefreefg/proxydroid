package me.bwelco.proxy.interceptor

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import java.io.IOException

interface Interceptor {

    fun intercept(chain: Chain): FullHttpResponse

    interface Chain {
        fun request(): FullHttpRequest

        fun proceed(request: FullHttpRequest): FullHttpResponse

        fun clientContext(): ChannelHandlerContext
    }
}
