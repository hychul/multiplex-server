package com.hychul.multiplex.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.hychul.multiplex.server.handler.AcceptHandler;
import com.hychul.multiplex.server.handler.AsyncProcessHandler;
import com.hychul.multiplex.server.handler.Handler;
import com.hychul.multiplex.server.handler.SyncProcessHandler;

public class NioEventLoopServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerSocketChannel serverSocketChannel;

    public NioEventLoopServer(int port) throws IOException {
        bossGroup = new EventLoopGroup();
        bossGroup.register(new EventLoop());
        workerGroup = new EventLoopGroup();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(bossGroup.next().selector, SelectionKey.OP_ACCEPT)
                           .attach((Handler) () -> {
                               SocketChannel socketChannel = this.serverSocketChannel.accept();
                               if (socketChannel == null) {
                                   return;
                               }

                               new SyncProcessHandler(workerGroup.next().selector, socketChannel);

                               System.out.println(String.format("[%s] %s", Thread.currentThread().getName(), "new client accepted"));
                           });
    }

    public void start() throws IOException {
        System.out.println(String.format("[%s] %s: %s", Thread.currentThread().getName(), "listening port", serverSocketChannel.socket().getLocalPort()));

        new Thread(null, bossGroup.next(), "boss-thread").start();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            EventLoop worker = new EventLoop(Selector.open());
            workerGroup.register(worker);
            new Thread(null, worker, "worker-thread-" + i).start();
        }
    }

    class EventLoop implements Runnable {
        private Selector selector;

        EventLoop() throws IOException {
            selector = Selector.open();
        }

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

    class EventLoopGroup {
        Queue<EventLoop> eventLoopQueue = new LinkedList<>();

        void register(EventLoop eventLoop) {
            eventLoopQueue.add(eventLoop);
        }

        synchronized EventLoop next() {
            EventLoop next = eventLoopQueue.poll();
            eventLoopQueue.add(next);
            return next;
        }
    }
}
