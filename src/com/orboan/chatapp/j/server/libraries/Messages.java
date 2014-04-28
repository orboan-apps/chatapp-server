/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.orboan.chatapp.j.server.libraries;

import com.orboan.chatapp.j.shared.model.Message;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public class Messages {
    private static final Message SERVER_ERROR_ON_READ_MESSAGE = 
            new Message(Message.Type.ERROR,
            Message.Sender.SERVER.toString(),
            "SERVER ERROR ON READ MESSAGE",
            Message.Recipient.CLIENT.toString());
    
    
    private static final Message SERVER_INFO_NO_MORE_MESSAGES = 
            new Message(Message.Type.NO_MORE_MESSAGES,
                    Message.Sender.SERVER.toString(), 
                    "NO MORE MESSAGES TO DELIVER", 
                    Message.Recipient.CLIENT.toString());
    
    private static final Message SERVER_INFO_LOGIN_OK = 
            new Message(Message.Type.LOGIN,
            Message.Sender.SERVER.toString(),
            "LOGIN SUCCESSFUL",
            Message.Recipient.CLIENT.toString());
    
    private static final Message SERVER_ERROR_LOGIN_KO = 
            new Message(Message.Type.LOGIN,
            Message.Sender.SERVER.toString(),
            "LOGIN FAILED",
            Message.Recipient.CLIENT.toString());  
    
    private static final Message SERVER_INFO_LOGOUT_OK = 
            new Message(Message.Type.LOGOUT,
            Message.Sender.SERVER.toString(),
            "LOGOUT SUCCESSFUL",
            Message.Recipient.CLIENT.toString());    
    
    private static final Message SERVER_INFO_CONNECTED_OK = 
            new Message(Message.Type.CONNECTION,
                Message.Sender.CHATAPP.toString(),
                "YOU ARE SUCCESSFULLY CONNECTED",
                Message.Recipient.CLIENT.toString());       
    
    public static Message getSERVER_ERROR_ON_READ_MESSAGE() {
        return SERVER_ERROR_ON_READ_MESSAGE;
    }        

    public static Message getSERVER_INFO_NO_MORE_MESSAGES() {
        return SERVER_INFO_NO_MORE_MESSAGES;
    }

    public static Message getSERVER_INFO_LOGIN_OK() {
        return SERVER_INFO_LOGIN_OK;
    }

    public static Message getSERVER_ERROR_LOGIN_KO() {
        return SERVER_ERROR_LOGIN_KO;
    }

    public static Message getSERVER_INFO_LOGOUT_OK() {
        return SERVER_INFO_LOGOUT_OK;
    }

    public static Message getSERVER_INFO_CONNECTED_OK() {
        return SERVER_INFO_CONNECTED_OK;
    }
}
