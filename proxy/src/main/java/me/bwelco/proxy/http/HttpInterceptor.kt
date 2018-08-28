package me.bwelco.proxy.http

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

interface HttpInterceptor {
    fun onRequest(request: FullHttpRequest): FullHttpRequest
    fun onResponse(response: FullHttpResponse): FullHttpResponse
}