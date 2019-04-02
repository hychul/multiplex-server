package com.hychul.multiplex.server.handler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler implements Handler {
    private final Selector selector;
    private final ServerSocketChannel socketChannel;

    public AcceptHandler(Selector selector, ServerSocketChannel socketChannel) {
        this.selector = selector;
        this.socketChannel = socketChannel;
    }

    public void handle() {
        try {
            SocketChannel socketChannel = this.socketChannel.accept();
            if (socketChannel == null) {
                return;
            }

            new ProcessHandler(selector, socketChannel);
            System.out.println(String.format("[%s] %s", Thread.currentThread().getName(), "new client accepted"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}