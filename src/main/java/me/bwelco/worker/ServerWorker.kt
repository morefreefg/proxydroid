package me.bwelco.worker

import me.bwelco.AccetpEvent
import me.bwelco.Connection
import me.bwelco.Event
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.LinkedBlockingQueue

class ServerWorker(val port: Int, val readProcessor: ReadProcessor) : Runnable {

    /**
     * start incoming socket ids from 16K - reserve bottom ids for pre-defined sockets (servers).
     */
    private var socketId: Long = 16 * 1024
    private lateinit var serverSocketChannel: ServerSocketChannel

    private lateinit var selector: Selector

    fun accept(selectionKey: SelectionKey) {
        val newSocketChannel = serverSocketChannel.accept()

        val newSocketId = socketId++
        val accetpEvent = AccetpEvent(Connection(newSocketChannel, newSocketId))

        newSocketChannel.configureBlocking(false)
        newSocketChannel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, accetpEvent.connection)
    }

    fun read(selectionKey: SelectionKey) {
        val connection = selectionKey.attachment() as Connection
        val socketChannel = connection.socketChannel
        readProcessor.read(connection) {
            selectionKey.cancel()
        }
    }

    fun write(selectionKey: SelectionKey) {

    }

    override fun run() {
        selector = Selector.open()


        this.serverSocketChannel = ServerSocketChannel.open()
        this.serverSocketChannel.configureBlocking(false)
        this.serverSocketChannel.socket().bind(InetSocketAddress(port))
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

        println("server start working at: $port")

        while (true) {
            selector.select()

            val iterator = selector.selectedKeys().iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                when {
                    !key.isValid -> return
                    key.isAcceptable -> accept(key)
                    key.isReadable -> read(key)
                    key.isWritable -> write(key)
                }
            }
        }
    }

}