package com.hychul.multiplex.server;

import com.hychul.multiplex.server.handler.AcceptHandler;
import com.hychul.multiplex.server.handler.Handler;
import com.hychul.multiplex.server.handler.ProcessHandlerType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

public class NioEventLoopServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerSocketChannel serverSocketChannel;

    public NioEventLoopServer(int port, boolean reactorMode) throws IOException {
        bossGroup = new EventLoopGroup("boss-group", 1);
        workerGroup = new EventLoopGroup("worker-group", Runtime.getRuntime().availableProcessors());

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(bossGroup.next().selector, SelectionKey.OP_ACCEPT)
                           .attach(new AcceptHandler(() -> workerGroup.next().selector,
                                                     serverSocketChannel,
                                                     reactorMode ? ProcessHandlerType.Reactor : ProcessHandlerType.Sync));
    }

    public void start() {
        System.out.println(String.format("[%s] %s: %s", Thread.currentThread().getName(), "listening port", serverSocketChannel.socket().getLocalPort()));

        bossGroup.run();
        workerGroup.run();
    }

    class EventLoop implements Runnable {
        private Selector selector;

        EventLoop() throws IOException {
            selector = Selector.open();
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

    class EventLoopGroup {
        String name;

        List<EventLoop> eventLoopList = new ArrayList<>();
        int picker = 0;

        EventLoopGroup(String name, int size) throws IOException {
            this.name = name;
            for (var i = 0; i < size; i++) {
                eventLoopList.add(new EventLoop());
            }
        }

        synchronized EventLoop next() {
            picker = picker % eventLoopList.size();
            return eventLoopList.get(picker++);
        }

        void run() {
            for (var i = 0; i < eventLoopList.size(); i++) {
                new Thread(null, eventLoopList.get(i), name + "-" + i).start();
            }
        }
    }
}
