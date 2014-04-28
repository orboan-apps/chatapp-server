/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.orboan.chatapp.j.server.model;

import com.orboan.chatapp.j.shared.model.Message;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public final class Attachment {

    private final int BUFSIZE = 1024; // Buffer size (bytes)    
    private String clientId;
    private ByteBuffer byteBuf;
    private Message message;
    private boolean sendingPendingLock = false;
    
    public static final String CONNECTION_SUCCESSFUL = "connectionSuccessful";
    public static final String LOGIN_SUCCESSFUL = "loginSuccessful";
    public static final String LOGIN_NOT_SUCCESSFUL = "loginNotSuccessful";
    public static final String LOGOUT_SUCCESSFUL = "logoutSuccessful";    
    public static final String RECIPIENT_IS_USER = "recipientIsUser";
    public static final String RECIPIENT_IS_ALL = "recipientIsAll";
    public static final String OPEN_PRIVATE = "openPrivate";
    public static final String CLOSE_PRIVATE = "closePrivate";
    
    private final HashMap<String, Boolean> responseState;
  
    
    public Attachment() {
        this.responseState = new HashMap<>();
        this.responseState.put(CONNECTION_SUCCESSFUL, false);
        this.responseState.put(LOGIN_SUCCESSFUL, false);
        this.responseState.put(LOGIN_NOT_SUCCESSFUL, false);
        this.responseState.put(LOGOUT_SUCCESSFUL, false);
        this.responseState.put(RECIPIENT_IS_USER, false);
        this.responseState.put(RECIPIENT_IS_ALL, false);
        this.responseState.put(OPEN_PRIVATE, false);
        this.responseState.put(CLOSE_PRIVATE, false);
        byteBuf = ByteBuffer.allocateDirect(BUFSIZE);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public ByteBuffer getByteBuf() {
        return this.byteBuf;
    }

    public void setByteBuf(ByteBuffer byteBuf) {
        this.byteBuf = byteBuf;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }    

    public HashMap<String, Boolean> getResponseState() {
        return responseState;
    }         

    public boolean isSendingPendingLock() {
        return sendingPendingLock;
    }

    public void setSendingPendingLock(boolean sendingPendingLock) {
        this.sendingPendingLock = sendingPendingLock;
    }        
}
