package me.bwelco;

import me.bwelco.netty.NettyServer;

public class Main {

    public static void main(String[] args) {
//        new Server().start(1080);
        new NettyServer(1080).startServer();
    }
}
