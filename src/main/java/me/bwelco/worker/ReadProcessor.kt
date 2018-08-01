package me.bwelco.worker

import java.sql.Connection

interface ReadProcessor {
    fun read(connection: Connection)
}