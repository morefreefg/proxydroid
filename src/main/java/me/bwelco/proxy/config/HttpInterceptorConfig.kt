package me.bwelco.proxy.config

import me.bwelco.proxy.http.HttpInterceptorMatcher


interface HttpInterceptorConfig {

    /**
     * enable ssl or not
     */
    fun enableMitm(): Boolean

    /**
     * select interceptor for different host
     */
    val httpInterceptorMatcher: HttpInterceptorMatcher

}