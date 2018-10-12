package me.bwelco.proxy.http

data class Remote(val host: String,
                  val port: Int,
                  val https: Boolean,
                  val url: String?)