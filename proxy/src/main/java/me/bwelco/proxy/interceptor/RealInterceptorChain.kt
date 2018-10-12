package me.bwelco.proxy.interceptor

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse


class RealInterceptorChain(val interceptors: List<Interceptor>,
                           val index: Int,
                           val request: FullHttpRequest,
                           val clientCtx: ChannelHandlerContext) : Interceptor.Chain {

    override fun clientContext(): ChannelHandlerContext = clientCtx

    override fun request(): FullHttpRequest = request

    var proceedBypassInternet = false

    override fun proceed(request: FullHttpRequest): FullHttpResponse {
        return proceedInternal(request)
    }

    private fun proceedInternal(request: FullHttpRequest): FullHttpResponse {
        if (index >= interceptors.size) {
            throw AssertionError()
        }

        // Call the next interceptor in the chain.
        val next = RealInterceptorChain(interceptors, index + 1, request, clientCtx)
        val interceptor = interceptors[index]
        if (interceptor is NetworkInterceptor) {
            proceedBypassInternet = true
        }
        return interceptor.intercept(next)
    }

}