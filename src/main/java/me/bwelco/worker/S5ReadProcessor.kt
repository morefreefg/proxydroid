package me.bwelco.worker

import me.bwelco.Connection
import java.io.IOException
import java.nio.ByteBuffer


class S5ReadProcessor: ReadProcessor {

    val readBuffer = ByteBuffer.allocate(1024)

    val sessionMap = mutableMapOf<Long, S5Session>()

    override fun read(connection: Connection, onError: (Exception) -> Unit) {
        readBuffer.clear()

        val socketChannel = connection.socketChannel

        var numRead = 0
        try {
            numRead = socketChannel.read(readBuffer)
        } catch (e: IOException) {
            onError(e)
            socketChannel.close()
            println("close by exception: $socketChannel")
            return
        }

        if (numRead == -1) {
            socketChannel.close()
            onError(RuntimeException("close by shutdown: $socketChannel"))
            println("close by shutdown: $socketChannel")
            return
        }

        val socketId = connection.socketId
        val session = sessionMap.get(socketId) ?: S5Session().apply { sessionMap.put(socketId, this) }
        val byteArray = ByteArray(numRead) { -1 }

        readBuffer.flip()
        readBuffer.get(byteArray)
        session.newData(byteArray)
    }

}