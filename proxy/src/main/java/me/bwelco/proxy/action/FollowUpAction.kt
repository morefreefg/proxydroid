package me.bwelco.proxy.action

import io.netty.channel.Channel

interface FollowUpAction {
    fun doFollowUp(clientChannel: Channel, remoteChannel: Channel)
}