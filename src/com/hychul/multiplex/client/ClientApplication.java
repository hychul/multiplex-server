package com.hychul.multiplex.client;

import java.io.IOException;

public class ClientApplication {
    public static void main(String[] args) throws IOException {
        Client client = new Client("127.0.0.1", 9900);
        client.start();
    }
}
