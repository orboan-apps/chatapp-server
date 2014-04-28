/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.orboan.chatapp.j.server.integration.sockets;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public interface IProtocol {

    void handleAccept(SelectionKey key) throws IOException;

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;

}
