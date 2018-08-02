package me.bwelco.worker

import me.bwelco.Connection


interface ReadProcessor {
    fun read(connection: Connection, onError: (Exception) -> Unit)
}