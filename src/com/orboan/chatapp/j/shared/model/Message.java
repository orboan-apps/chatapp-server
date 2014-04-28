/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.orboan.chatapp.j.shared.model;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String EOL = "\n";

    private final Type type;
    private final String content, recipient;
    private String sender;
    private char[] password;
    private ArrayList<String> connectedUsers;

    public Type getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }        

    public ArrayList<String> getConnectedUsers() {
        return connectedUsers;
    }  

    public void setConnectedUsers(ArrayList<String> connectedUsers) {
        this.connectedUsers = connectedUsers;
    }    
    
    public Message(String content) {
        this.type = Type.LOG;
        this.sender = Message.Sender.CHATAPP.toString();
        this.content = content; 
        this.recipient = Message.Recipient.SERVER.toString();
    }

    public Message(Type type, String sender, String content, String recipient) {
        this.type = type;
        this.sender = sender;
        this.content = content + EOL;
        this.recipient = recipient;
    }

    public Message(Message message) {
        this.type = message.type;
        this.sender = message.sender;
        this.content = message.content;
        this.recipient = message.recipient;
    }

    @Override
    public String toString() {
        //Remove EOL from contents
        String contents = new StringBuilder(this.content).substring(0, this.content.length() - 1);
        return "{type='" + type + "' sender='" + sender + "' content='" + contents + "' recipient='" + recipient + "'}";
    }

    public enum Type {

        MESSAGE(100), WARNING(101), ERROR(102), CONNECTION(103),
        LOGIN(110), LOGIN_FROM(111), LOGOUT(112), SIGNUP(113), BYE(114),
        UPLOAD_REQ(120), NO_MORE_MESSAGES(121), MESSAGE_ALL(201),
        WELCOME(122), OPEN_PRIVATE(133), CLOSE_PRIVATE(134), LOG(199);
        private final int type;

        Type(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }
    }

    public enum Recipient {

        CLIENT("CLIENT"), SERVER("SERVER"), CHATAPP("ChatApp"),
        ALL("All");
        private final String recipient;

        Recipient(String recipient) {
            this.recipient = recipient;
        }

        @Override
        public String toString() {
            return this.recipient;
        }
    }
    
    public enum Sender {

        CLIENT("CLIENT"), SERVER("SERVER"), CHATAPP("ChatApp");
        private final String sender;

        Sender(String sender) {
            this.sender = sender;
        }

        @Override
        public String toString() {
            return this.sender;
        }
    }    

    public char[] getPassword() {        
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void clearPassword() {
        for (int i = 0; i < password.length; i++) {
            password[i] = ' ';
        }
    }
}