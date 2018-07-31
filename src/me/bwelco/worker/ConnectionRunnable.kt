package me.bwelco.worker

import me.bwelco.Connection
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue

class ConnectionRunnable(val connectionQueue: LinkedBlockingQueue<Connection>): Runnable {


    private val socketMap = mutableMapOf<Long, SocketChannel>()
    private lateinit var readSelector: Selector
    private lateinit var writeSelector: Selector

    fun initialize() {
        readSelector = Selector.open()
        writeSelector = Selector.open()

    }

    fun takeNewSocketChannel() {
        val newConnection = connectionQueue.take()

        val socketChannel = newConnection.socketChannel
        socketChannel.configureBlocking(false)
        socketChannel.register(this.readSelector, SelectionKey.OP_READ, newConnection)
        socketChannel.register(this.writeSelector, SelectionKey.OP_WRITE, newConnection)

        this.socketMap.put(newConnection.sockerId, newConnection.socketChannel)


    }

    override fun run() {
        initialize()

        while (true) {

        }
    }

}