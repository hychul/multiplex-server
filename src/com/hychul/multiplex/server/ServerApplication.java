package com.hychul.multiplex.server;

import java.io.IOException;

public class ServerApplication {
    public static void main(String[] args) throws IOException {
        Server server = new Server(9900);
        server.start();
    }
}
