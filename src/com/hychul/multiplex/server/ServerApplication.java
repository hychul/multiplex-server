package com.hychul.multiplex.server;

import java.io.IOException;

public class ServerApplication {
    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer(9900, true);
        nioServer.start();
    }
}
