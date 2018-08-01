package me.bwelco

sealed class Event(val connection: Connection)

class AccetpEvent(connection: Connection): Event(connection)
class ReadableEvent(connection: Connection): Event(connection)
class WriteableEvent(connection: Connection): Event(connection)