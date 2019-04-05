package com.hychul.multiplex.server.handler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AsyncProcessHandler extends ProcessHandler {
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        int threadCount = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(null, r, "reactive-thread-" + threadCount++);
        }
    });

    public AsyncProcessHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        super(selector, socketChannel);
    }

    @Override
    protected void onRead(int readCount) {
        pool.execute(new Processor(readCount));
    }

    class Processor implements Runnable {
        private int readCount;

        Processor(int readCount) {
            this.readCount = readCount;
        }

        @Override
        public synchronized void run() {
            process(readCount);
        }
    }
}
