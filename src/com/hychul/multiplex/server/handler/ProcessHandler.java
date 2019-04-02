package com.hychul.multiplex.server.handler;

import static com.hychul.multiplex.server.handler.HandlerState.PROCESSING;
import static com.hychul.multiplex.server.handler.HandlerState.READING;
import static com.hychul.multiplex.server.handler.HandlerState.WRITING;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ProcessHandler implements Handler {

    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        int threadCount = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(null, r, "reactive-thread-" + threadCount++);
        }
    });

    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;

    private HandlerState state = READING;

    private ByteBuffer input = ByteBuffer.allocate(1024);
    private String message = "";

    public ProcessHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        selectionKey = this.socketChannel.register(selector, 0);
        selectionKey.attach(this);
        selectionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void handle() throws IOException {
        if (state == READING) {
            read();
        } else if (state == WRITING) {
            write();
        }
    }

    private void read() throws IOException {
        System.out.println("read " + Thread.currentThread().getName());

        int readCount = socketChannel.read(input);
        if (0 < readCount) {
            state = PROCESSING;
            pool.execute(new Processor(readCount));
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        } else if (readCount < 0) {
            socketChannel.close();
        }
    }

    private void write() throws IOException {
        System.out.println("write " + Thread.currentThread().getName());

        System.out.print("write message: " + message);

        ByteBuffer output = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(output);

        state = READING;
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    class Processor implements Runnable {
        private int readCount;

        Processor(int readCount) {
            this.readCount = readCount;
        }

        public synchronized void run() {
            System.out.println("process " + Thread.currentThread().getName());

            StringBuilder sb = new StringBuilder();
            input.flip();
            byte[] subStringBytes = new byte[readCount];
            byte[] array = input.array();
            System.arraycopy(array, 0, subStringBytes, 0, readCount);
            sb.append(new String(subStringBytes));
            input.clear();
            message = sb.toString();

            state = WRITING;
        }
    }
}
