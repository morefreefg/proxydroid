package me.bwelco.proxy.http

interface HttpInterceptorMatcher {
    fun match(host: String): HttpInterceptor?
}
