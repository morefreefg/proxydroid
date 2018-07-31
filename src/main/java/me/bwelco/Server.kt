package me.bwelco

import me.bwelco.worker.ServerEventProdiver
import me.bwelco.worker.ConnectionRunnable
import java.util.concurrent.LinkedBlockingQueue

class Server {

    companion object {
        val connectionQueue = LinkedBlockingQueue<Connection>()
    }

    fun start(port: Int) {
        Thread(ServerEventProdiver(port, connectionQueue)).start()
        Thread(ConnectionRunnable(connectionQueue)).start()
    }
}