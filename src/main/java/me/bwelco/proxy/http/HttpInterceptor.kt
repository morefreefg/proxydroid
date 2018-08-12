package me.bwelco.proxy.http

interface HttpInterceptor {

    fun intercept(chain: Chain): HttpResponse

    interface Chain {
        fun request(): HttpRequest

        fun proceed(request: HttpRequest): HttpResponse
    }
}