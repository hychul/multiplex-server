package com.hychul.multiplex.server.handler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SyncProcessHandler extends ProcessHandler {

    public SyncProcessHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        super(selector, socketChannel);
    }

    @Override
    protected void onRead(int readCount) {
        process(readCount);
    }
}
