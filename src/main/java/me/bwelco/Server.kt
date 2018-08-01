package me.bwelco

import me.bwelco.worker.S5ReadProcessor
import me.bwelco.worker.ServerWorker
import java.util.concurrent.LinkedBlockingQueue

class Server {

    fun start(port: Int) {
        Thread(ServerWorker(port, S5ReadProcessor())).start()
    }
}