package com.hychul.multiplex.server;

import java.io.IOException;
import java.util.Scanner;

public class ServerApplication {
    public static void main(String[] args) throws IOException {
        System.out.println("[1. NioSyncServer | 2. NioAsyncServer | 3. NioEventLoopServer | 4.NioEventLoopReactorServer]");
        System.out.print("Select server mode : ");
        Scanner scanner = new Scanner(System.in);
        switch (scanner.nextInt()) {
            case 1:
                new NioServer(9900, false).start();
                break;
            case 2:
                new NioServer(9900, true).start();
                break;
            case 3:
                new NioEventLoopServer(9900, false).start();
                break;
            case 4:
                new NioEventLoopServer(9900, true).start();
                break;
            default:
                System.out.println("Wrong input");
                break;
        }
    }
}
