package me.bwelco

import java.nio.channels.SocketChannel

data class Connection(val socketChannel: SocketChannel,
                      val socketId: Long)