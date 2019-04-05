package com.hychul.multiplex.server.handler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

public class AcceptHandler implements Handler {
    private final Supplier<Selector> selectorSupplier;
    private final ServerSocketChannel socketChannel;
    private final ProcessHandlerType processHandlerType;

    public AcceptHandler(Supplier<Selector> selectorSupplier, ServerSocketChannel socketChannel, ProcessHandlerType processHandlerType) {
        this.selectorSupplier = selectorSupplier;
        this.socketChannel = socketChannel;
        this.processHandlerType = processHandlerType;
    }

    @Override
    public void handle() throws IOException {
        SocketChannel socketChannel = this.socketChannel.accept();
        if (socketChannel == null) {
            return;
        }

        switch (processHandlerType) {
            case Sync:
                new SyncProcessHandler(selectorSupplier.get(), socketChannel);
                break;
            case Async:
                new AsyncProcessHandler(selectorSupplier.get(), socketChannel);
                break;
            case Reactor:
                new ReactorProcessHandler(selectorSupplier.get(), socketChannel, it -> Mono.just(it).log());
                break;
        }

        System.out.println(String.format("[%s] %s", Thread.currentThread().getName(), "new client accepted"));
    }
}