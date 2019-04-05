package com.hychul.multiplex.server.handler;

import static com.hychul.multiplex.server.handler.HandlerState.WRITING;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public class ReactorProcessHandler extends ProcessHandler {
    private Function<String, Mono<String>> function;

    public ReactorProcessHandler(Selector selector, SocketChannel socketChannel, Function<String, Mono<String>> function) throws IOException {
        super(selector, socketChannel);
        this.function = function;
    }

    @Override
    protected void onRead(int readCount) {
        process(readCount);
    }

    @Override
    protected void process(int readCount) {
        super.process(readCount);
        Mono<String> result = function.apply(message);
        result.subscribe(it -> {
            message = it;
            state = WRITING;
        });
    }
}
