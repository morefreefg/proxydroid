package me.bwelco.proxy.http

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpVersion

data class HttpRequest(val httpVersion: HttpVersion,
                       val httpProtocolVersion: HttpVersion,
                       val headers: HttpHeaders,
                       val httpUrl: String,
                       val requestBody: ByteBuf)
