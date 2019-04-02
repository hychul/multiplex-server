package com.hychul.multiplex.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import com.hychul.multiplex.server.handler.AcceptHandler;
import com.hychul.multiplex.server.handler.Handler;

public class Server {
    private final Selector parentSelector;
    private final Selector childSelector;
    private final ServerSocketChannel serverSocketChannel;

    public Server(int port) throws IOException {
        parentSelector = Selector.open();
        childSelector = Selector.open();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(parentSelector, SelectionKey.OP_ACCEPT).attach(new AcceptHandler(childSelector, serverSocketChannel));
    }

    public void start() {
        System.out.println(String.format("[%s] %s: %s", Thread.currentThread().getName(), "listening port", serverSocketChannel.socket().getLocalPort()));
        new Thread(null, new EventLoop(parentSelector), "acceptor-thread").start();
        new Thread(null, new EventLoop(childSelector), "dispatcher-thread").start();
    }

    class EventLoop implements Runnable {
        Selector selector;

        EventLoop(Selector selector) {
            this.selector = selector;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    selector.selectedKeys().forEach(this::process);
                    selector.selectedKeys().clear();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void process(SelectionKey key) {
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
