package me.bwelco.worker

import jdk.management.resource.internal.inst.SocketRMHooks
import me.bwelco.Connection
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue

class ServerEventProdiver(val port: Int, val queue: LinkedBlockingQueue<Connection>) : Runnable {

    /**
     * start incoming socket ids from 16K - reserve bottom ids for pre-defined sockets (servers).
     */
    private var socketId: Long = 16 * 1024
    private lateinit var serverSocketChannel: ServerSocketChannel

    private lateinit var selector: Selector
	private val readBuffer = ByteBuffer.allocate(8912);

    fun accept(selectionKey: SelectionKey) {
        val socketChannel = serverSocketChannel.accept()
        println(socketChannel)
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, socketId++)

    }

    fun read(selectionKey: SelectionKey) {
        val socketChannel = selectionKey.channel() as SocketChannel
        readBuffer.clear()

        var numRead = 0
        try {
            numRead = socketChannel.read(readBuffer)
        } catch (e: IOException) {
            selectionKey.cancel()
            socketChannel.close()
            println("close by exception: $socketChannel")
            return
        }

        if (numRead == -1) {
            socketChannel.close()
            selectionKey.cancel()
            println("close by shutdown: $socketChannel")
            return
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