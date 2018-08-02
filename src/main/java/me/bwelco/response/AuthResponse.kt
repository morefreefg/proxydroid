package me.bwelco.response

class AuthResponse(val methodType: Int) : Response {

    private val SOCKS_VERSION_5 = 0x05

    override fun toResponse(): ByteArray = byteArrayOf(SOCKS_VERSION_5.toByte(), methodType.toByte())

}