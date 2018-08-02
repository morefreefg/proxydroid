package me.bwelco.exception

class SocksVersionNotSupport(version: Int): Throwable("this socks version: $version not supported")