package me.bwelco.worker

import me.bwelco.*
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue

class ConnectionRunnable(val eventQueue: LinkedBlockingQueue<Event>): Runnable {

    val connectionMap = mutableMapOf<Long, SocketChannel>()

    fun initialize() {

    }

    fun newConnection(accetpEvent: AccetpEvent) {
        connectionMap.put(accetpEvent.connection.socketId, accetpEvent.connection.socketChannel)
        println("new connection: ${accetpEvent.connection.socketChannel}")
    }

    fun newReadableEvent(readableEvent: ReadableEvent) {

    }

    fun newWriteableEvent(writeableEvent: WriteableEvent) {

    }

    override fun run() {
        initialize()

        while (true) {
            val event = eventQueue.take()

            when (event) {
                is AccetpEvent -> newConnection(event)
                is ReadableEvent -> newReadableEvent(event)
                is WriteableEvent -> newWriteableEvent(event)
            }
        }
    }

}