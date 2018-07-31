package me.bwelco

sealed class Event()

class AccetpEvent: Event() {

}

class ReadableEvent: Event() {

}

class WriteableEvent: Event() {

}