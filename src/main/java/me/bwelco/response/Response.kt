package me.bwelco.response


interface Response {
    fun toResponse(): ByteArray
}