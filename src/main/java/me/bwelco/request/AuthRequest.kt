package me.bwelco.request

import me.bwelco.exception.SocksVersionNotSupport

class AuthRequest: Request {
    private val SOCKS_VERSION_5 = 0x05

    var methodNum: Int = 0
    private lateinit var methodArray: MutableList<Int>

    override fun fromRequest(data: ByteArray) {
        val socksVersion = data[0].toInt()
        if (socksVersion != SOCKS_VERSION_5) {
            throw SocksVersionNotSupport(socksVersion)
        }

        methodNum = data[1].toInt()
        methodArray = ArrayList(methodNum)

        for (index in 0..methodNum) {
            methodArray[index] = data[2 + index].toInt()
        }
    }
}