/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.orboan.chatapp.j.server.main;

import com.orboan.chatapp.j.server.integration.sockets.ServerSelector;
import java.io.IOException;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public class Main {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        new ServerSelector();
    }
    
}
