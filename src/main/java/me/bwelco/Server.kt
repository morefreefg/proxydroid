package me.bwelco

import me.bwelco.worker.ServerEventProdiver
import me.bwelco.worker.ConnectionRunnable
import java.util.concurrent.LinkedBlockingQueue

class Server {

    companion object {
        val eventQueue = LinkedBlockingQueue<Event>()
    }

    fun start(port: Int) {
        Thread(ServerEventProdiver(port, eventQueue)).start()
        Thread(ConnectionRunnable(eventQueue)).start()
    }
}