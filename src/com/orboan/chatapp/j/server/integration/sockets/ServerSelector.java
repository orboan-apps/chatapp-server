package com.orboan.chatapp.j.server.integration.sockets;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import static com.orboan.chatapp.j.server.model.ServerSettings.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public class ServerSelector {

    public ServerSelector() throws IOException {

        // Create a selector to multiplex listening sockets and connections
        Selector selector = Selector.open();
        // Create listening socket channel for each port and register selector
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        InetSocketAddress socketAddress = new InetSocketAddress(PORT);
        serverChannel.socket().bind(socketAddress);
        serverChannel.configureBlocking(false); // must be nonblocking to register
        // Register selector with channel. The returned key is ignored
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // Create a handler that will implement the protocol
        IProtocol protocol = new ChatSelectorProtocol();
        System.out.print("CHATAPP SERVER IS LISTENING [" + socketAddress.toString() + "] ");
        while (true) { // Run forever, processing available I/O operations
        // Wait for some channel to be ready (or timeout)
            if (selector.select(TIMEOUT) == 0) { // returns # of ready chans
                System.out.print(".");
                continue;
            }
            // Get iterator on set of keys with I/O to process
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next(); // Key is bit mask
                keyIter.remove(); // remove from set of selected keys
                // Server socket channel has pending connection requests?
                if (key.isAcceptable()) {
                    protocol.handleAccept(key);
                }
                // Client socket channel has pending data?
                if (key.isReadable()) {
                    protocol.handleRead(key);
                }
                // Client socket channel is available for writing and
                // key is valid (i.e., channel not closed)?
                if (key.isValid() && key.isWritable()) {
                    protocol.handleWrite(key);
                }
            }
        }
    }
}
