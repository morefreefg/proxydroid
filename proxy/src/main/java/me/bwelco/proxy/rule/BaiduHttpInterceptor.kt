package me.bwelco.proxy.rule

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import me.bwelco.proxy.http.HttpInterceptor

class BaiduHttpInterceptor : HttpInterceptor {

    override fun onRequest(request: FullHttpRequest): FullHttpRequest {
        return request
    }

    override fun onResponse(response: FullHttpResponse): FullHttpResponse {
        return response.replace(Unpooled.wrappedBuffer(java.lang.String(
                "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "\t<title>fucking baidu</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "\t<h1>hooked html</h1>\n" +
                        "\n" +
                        "</body>\n" +
                        "</html>").getBytes())).apply {
            this.headers().remove("Content-Encoding")
        }
    }
}
