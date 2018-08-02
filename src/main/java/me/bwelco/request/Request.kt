package me.bwelco.request

interface Request {
    fun fromRequest(data: ByteArray)
}