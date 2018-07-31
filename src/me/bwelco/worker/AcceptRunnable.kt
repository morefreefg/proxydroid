package me.bwelco.worker

import me.bwelco.Connection
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.LinkedBlockingQueue

class AcceptRunnable(val port: Int, val queue: LinkedBlockingQueue<Connection>): Runnable {

    /**
     * start incoming socket ids from 16K - reserve bottom ids for pre-defined sockets (servers).
     */
    private var socketId: Long = 16 * 1024
    private lateinit var serverSocketChannel: ServerSocketChannel


    override fun run() {
        this.serverSocketChannel = ServerSocketChannel.open()
        this.serverSocketChannel.bind(InetSocketAddress(port))

        while (true) {
            val socketChannel = serverSocketChannel.accept()
            queue.put(Connection(socketChannel, socketId++))
        }
    }

}