package com.hychul.multiplex.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import com.hychul.multiplex.server.handler.AcceptHandler;
import com.hychul.multiplex.server.handler.Handler;

public class NioServer {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    public NioServer(int port, boolean asyncMode) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
                           .attach(new AcceptHandler(selector, serverSocketChannel, asyncMode));
    }

    public void start() {
        new Thread(null, new Dispatcher(), "dispatcher-thread").start();
    }

    class Dispatcher implements Runnable {
        @Override
        public void run() {
            System.out.println(String.format("[%s] %s: %s", Thread.currentThread().getName(), "listening port", serverSocketChannel.socket().getLocalPort()));
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    selector.selectedKeys().forEach(this::dispatch);
                    selector.selectedKeys().clear();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void dispatch(SelectionKey key) {
            var handler = (Handler) key.attachment();

            try {
                handler.handle();
            } catch (IOException ex) {
                ex.printStackTrace();
                try {
                    key.channel().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
