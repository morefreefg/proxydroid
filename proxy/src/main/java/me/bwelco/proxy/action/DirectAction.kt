package me.bwelco.proxy.action

import io.netty.channel.Channel
import me.bwelco.proxy.upstream.RelayHandler

class DirectAction : FollowUpAction {

    override fun doFollowUp(clientChannel: Channel, remoteChannel: Channel) {
        clientChannel.pipeline().addLast(RelayHandler(remoteChannel))
        remoteChannel.pipeline().addLast(RelayHandler(clientChannel))
    }

}