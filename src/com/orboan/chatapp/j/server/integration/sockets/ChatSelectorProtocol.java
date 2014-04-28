/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.orboan.chatapp.j.server.integration.sockets;

import com.orboan.chatapp.j.server.model.ServerSettings;
import com.orboan.chatapp.j.server.model.Attachment;
import com.orboan.chatapp.j.shared.model.Message;
import com.orboan.chatapp.j.server.libraries.Messages;
import com.orboan.chatapp.j.utils.SocketUtils;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public class ChatSelectorProtocol implements IProtocol {

    private final ConcurrentHashMap<String, ArrayList<Message>> messages;
    private final ConcurrentHashMap<String, ArrayList<SocketChannel>> clients;
    private final int numberOfBytesAsMessageLength = 4;

    public ChatSelectorProtocol() {
        this.messages = new ConcurrentHashMap<>(64);
        this.clients = new ConcurrentHashMap<>(64);
    }

    @Override
    public void handleAccept(SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        this.registerClient(key, clientChannel);

        //Log the accepted connection
        Message logMessage = new Message("Got connection from: "
                + clientChannel.socket().getInetAddress());
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
        this.setResponseState(Attachment.CONNECTION_SUCCESSFUL,
                attachment, Messages.getSERVER_INFO_CONNECTED_OK(), logMessage);
        this.handleResponse(key, clientChannel);

    }

    private void sendBroadcastLoginMessage(SelectionKey key, SocketChannel clientChannel, String content) {
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
        ByteBuffer buf = attachment.getByteBuf();
        //Send broadcast message
        Message m2All = new Message(Message.Type.LOGIN_FROM,
                attachment.getClientId(),
                content,
                Message.Recipient.ALL.toString());
        m2All.setConnectedUsers(new ArrayList<>(clients.keySet()));
        // private void sendBroadcastMessage(ByteBuffer buf, Message message, SocketChannel from);          
        this.sendBroadcastMessage(buf, m2All, clientChannel);
    }

    private void sendLoginMessage(SelectionKey key, SocketChannel clientChannel,
            String content) {
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
        ByteBuffer buf = attachment.getByteBuf();
        //Send message back
        Message msgBack = new Message(Message.Type.WELCOME,
                attachment.getClientId(),
                content, Message.Recipient.CLIENT.toString());
        msgBack.setConnectedUsers(new ArrayList<>(clients.keySet()));
        //private void sendMessage(SocketChannel clientChannel, ByteBuffer buf, Message message);  
        this.sendMessage(clientChannel, buf, msgBack);
    }

    private void sendResponseToSuccessfulLogin(SelectionKey key, SocketChannel clientChannel) {
        //Inform clients about the new accepted connection:
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();

        sendBroadcastLoginMessage(key, clientChannel,
                attachment.getClientId() + " has logged in from "
                + clientChannel.socket().getInetAddress());

        sendLoginMessage(key, clientChannel,
                "Welcome " + attachment.getClientId() + ". There are " + clients.size() + " users online.");

    }

    private void sendResponseToSuccessfulLogout(SelectionKey key, SocketChannel clientChannel) {
        //Inform clients about the new logged out user:
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();

        sendBroadcastLoginMessage(key, clientChannel,
                attachment.getClientId() + " has logged out from "
                + clientChannel.socket().getInetAddress());

        sendLoginMessage(key, clientChannel,
                "Good bye " + attachment.getClientId() + ".");

        attachment.setClientId(null);
    }

    private int getMessageLength(SocketChannel clientChannel) {
        int len = 0;
        ByteBuffer buf = ByteBuffer.allocate(numberOfBytesAsMessageLength);
        int totalBytesRead = 0;
        int bytesRead;
        while (totalBytesRead < numberOfBytesAsMessageLength) {
            try {
                if ((bytesRead = clientChannel.read(buf)) == -1) {
                    throw new SocketException();
                }
                totalBytesRead += bytesRead;
            } catch (IOException ex) {
                try {
                    Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.SEVERE, "Connection closed prematurely by {0}", clientChannel.getRemoteAddress());
                } catch (IOException ex1) {
                    Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.SEVERE, null, ex1);
                }
                return -1;
            }
        }
        buf.flip();
        len = buf.getInt();
        return len;
    }

    private Message receiveMessage(SocketChannel clientChannel,
            Attachment attachment, int len) throws IOException {
        Message m;
        ByteBuffer buf = attachment.getByteBuf();
        if (buf.capacity() < len) {
            buf = ByteBuffer.allocate(len);
            attachment.setByteBuf(buf);
        } else {
            buf.clear();
            buf = (ByteBuffer) buf.limit(len);
        }
        int totalBytesRead = 0;
        while (totalBytesRead < len) {
            int bytesRead = clientChannel.read(buf);
            if (bytesRead == -1) { // Did the other end close?
                Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.INFO,
                        "disconnect: {0}, end-of-stream", clientChannel.socket().getInetAddress());
                ArrayList<SocketChannel> clientChannels;
                if ((clientChannels = this.clients.get(attachment.getClientId())) != null) {
                    clientChannels.remove(clientChannel);
                }
                if (clientChannels.isEmpty()) {
                    this.clients.remove(attachment.getClientId());
                }
                clientChannel.close();
            } else if (bytesRead > 0) {
                totalBytesRead += bytesRead;
            }
        }
        if (totalBytesRead == len) {
            buf.flip();
            byte[] array;
            if (buf.hasArray()) {
                array = buf.array();
            } else {
                array = new byte[buf.remaining()];
                buf.get(array);
            }
            Object o = SocketUtils.bytesToObject(array);
            if (o instanceof Message) {
                m = (Message) o;
                System.out.println("SERVER: Received: " + m.toString());
            } else {
                m = Messages.getSERVER_ERROR_ON_READ_MESSAGE();
                Logger.getLogger(ChatSelectorProtocol.class.getName()).log(
                        Level.SEVERE, "{0} : {1}", new String[]{m.getSender(), m.getContent()});
            }
        } else {
            m = Messages.getSERVER_ERROR_ON_READ_MESSAGE();
            Logger.getLogger(ChatSelectorProtocol.class.getName()).log(
                    Level.SEVERE, "{0} : {1}", new String[]{m.getSender(), m.getContent()});
        }
//        attachment.setClientId(m.getSender());
        return m;
    }

    private boolean authenticate(String userid) {
        //TODO
        return true;
    }

    private void setResponseState(String responseType, Attachment attachment,
            Message responseMessage, Message serverLogMessage) {

        attachment.getResponseState().put(responseType, Boolean.TRUE);
        attachment.setMessage(responseMessage);
        Logger.getLogger(ChatSelectorProtocol.class.getName()).log(
                Level.INFO, "[Sender]{0} : [Content]{1} : [Recipient]{2}",
                new String[]{serverLogMessage.getSender(),
                    serverLogMessage.getContent(),
                    serverLogMessage.getRecipient()});
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
        //Get the length of the message to read
        int len = this.getMessageLength(clientChannel);

        //Read the message
        //Sets the attachment clientID
        if (len > 0) {
            Message messageReceived = this.receiveMessage(clientChannel, attachment, len);
            Message logMessage;
            String recipient = messageReceived.getRecipient();
            Message.Type type = messageReceived.getType();

            if (recipient.equals(Message.Recipient.SERVER.toString())) {
                if (type.equals(Message.Type.LOGIN)) {
                    if (this.authenticate(messageReceived.getSender())) {
                        ArrayList<SocketChannel> clientChannels;
                        if ((clientChannels = this.clients.get(messageReceived.getSender())) == null) {
                            clientChannels = new ArrayList<>();
                        }
                        clientChannels.add(clientChannel);
                        this.clients.put(messageReceived.getSender().trim(), clientChannels);
                        attachment.setClientId(messageReceived.getSender());
                        logMessage = new Message("User " + messageReceived.getSender()
                                + " has sucessfully logged in.");
                        Message m = Messages.getSERVER_INFO_LOGIN_OK();
                        this.setResponseState(Attachment.LOGIN_SUCCESSFUL,
                                attachment, m, logMessage);
                    } else {
                        attachment.setClientId(null);
                        logMessage = new Message("User " + messageReceived.getSender()
                                + " has failed to log in.");
                        this.setResponseState(Attachment.LOGIN_NOT_SUCCESSFUL,
                                attachment, Messages.getSERVER_ERROR_LOGIN_KO(), logMessage);
                    }
                } else if (type.equals(Message.Type.LOGOUT)
                        || type.equals(Message.Type.BYE)) {
                    if (attachment.getClientId() != null) {
                        logMessage = new Message("User " + messageReceived.getSender()
                                + " has sucessfully logged out.");
                        Message m = Messages.getSERVER_INFO_LOGOUT_OK();
                        m.setSender(messageReceived.getSender());

                        ArrayList<SocketChannel> clientChannels;
                        if ((clientChannels = this.clients.get(attachment.getClientId())) != null) {
                            clientChannels.remove(clientChannel);
                        }
                        if (clientChannels.isEmpty()) {
                            this.clients.remove(attachment.getClientId());
                        }

//                        attachment.setClientId(null); //Deferred after the back message
                        this.setResponseState(Attachment.LOGOUT_SUCCESSFUL,
                                attachment, m, logMessage);
                    }
                }
            } else if (!recipient.equals(Message.Recipient.ALL.toString())
                    && !recipient.equals(Message.Recipient.CLIENT.toString())) {
                if (type.equals(Message.Type.MESSAGE)) {
                    if (attachment.getClientId() != null) {
                        logMessage = new Message("User " + messageReceived.getSender()
                                + " is sending the content \""
                                + messageReceived.getContent() + "\" to " + messageReceived.getRecipient()
                                + ".");
                        this.setResponseState(Attachment.RECIPIENT_IS_USER,
                                attachment, messageReceived, logMessage);
                    }
                } else if (type.equals(Message.Type.OPEN_PRIVATE)) {
                    if (attachment.getClientId() != null) {
                        logMessage = new Message(messageReceived.getContent().replace(Message.EOL, ""));
                        this.setResponseState(Attachment.OPEN_PRIVATE,
                                attachment, messageReceived, logMessage);
                    }
                } else if (type.equals(Message.Type.CLOSE_PRIVATE)) {
                    if (attachment.getClientId() != null) {
                        logMessage = new Message(messageReceived.getContent());
                        this.setResponseState(Attachment.CLOSE_PRIVATE,
                                attachment, messageReceived, logMessage);
                    }
                }
            } else if (recipient.equals(Message.Recipient.ALL.toString())) {
                if (attachment.getClientId() != null) {
                    logMessage = new Message("User " + messageReceived.getSender()
                            + " is broadcasting the content \""
                            + messageReceived.getContent().replace(Message.EOL, "") + "\" to ALL.");
                    this.setResponseState(Attachment.RECIPIENT_IS_ALL,
                            attachment, messageReceived, logMessage);
                }
            }
            handleResponse(key);
        } else if (len == -1) {
            clientChannel.close();
        }
    }

    @Override
    public void handleWrite(SelectionKey key) throws IOException {
    }

    private void handleResponse(SelectionKey key) throws IOException {
        SocketChannel clientChannel
                = (SocketChannel) key.channel();
        this.handleResponse(key, clientChannel);
    }

    private void handleResponse(SelectionKey key, SocketChannel clientChannel) throws IOException {

        com.orboan.chatapp.j.server.model.Attachment attachment
                = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
        ByteBuffer buf = attachment.getByteBuf();

        Message m = attachment.getMessage();
        HashMap<String, Boolean> hm = attachment.getResponseState();
        ArrayList<SocketChannel> recipientChannels = null;
        if (m != null) {
            recipientChannels = this.clients.get(m.getRecipient());
        }

        if (hm.get(Attachment.RECIPIENT_IS_USER)) {
            if (m != null
                    && recipientChannels != null) {
                for (SocketChannel recipientChannel : recipientChannels) {
                    this.sendMessage(recipientChannel, buf, m);
                }
            }
            hm.put(Attachment.RECIPIENT_IS_USER, Boolean.FALSE);

        } else if (hm.get(Attachment.RECIPIENT_IS_ALL)) {
            if (m != null) {
                this.sendMessage(clientChannel, buf, m);
                this.sendBroadcastMessage(buf, m, clientChannel);
            }
            hm.put(Attachment.RECIPIENT_IS_ALL, Boolean.FALSE);

        } else if (hm.get(Attachment.OPEN_PRIVATE)) {
            if (m != null
                    && recipientChannels != null) {
                for (SocketChannel recipientChannel : recipientChannels) {
                    this.sendMessage(recipientChannel, buf, m);
                }
            }
            hm.put(Attachment.OPEN_PRIVATE, Boolean.FALSE);

        } else if (hm.get(Attachment.CLOSE_PRIVATE)) {
            if (m != null
                    && recipientChannels != null) {
                for (SocketChannel recipientChannel : recipientChannels) {
                    this.sendMessage(recipientChannel, buf, m);
                }
            }
            hm.put(Attachment.CLOSE_PRIVATE, Boolean.FALSE);

        } else if (hm.get(Attachment.CONNECTION_SUCCESSFUL)) {
            if (m != null) {
                this.sendMessage(clientChannel, buf, m);
            }
            hm.put(Attachment.CONNECTION_SUCCESSFUL, Boolean.FALSE);

        } else if (hm.get(Attachment.LOGIN_SUCCESSFUL)) {
            if (m != null) {
                m.setConnectedUsers(new ArrayList<>(clients.keySet()));
                this.sendMessage(clientChannel, buf, m);
            }
            this.sendResponseToSuccessfulLogin(key, clientChannel);
            hm.put(Attachment.LOGIN_SUCCESSFUL, Boolean.FALSE);

        } else if (hm.get(Attachment.LOGIN_NOT_SUCCESSFUL)) {
            if (m != null) {
                this.sendMessage(clientChannel, buf, m);
            }
            hm.put(Attachment.LOGIN_NOT_SUCCESSFUL, Boolean.FALSE);

        } else if (hm.get(Attachment.LOGOUT_SUCCESSFUL)) {
            if (m != null) {
                m.setConnectedUsers(new ArrayList<>(clients.keySet()));
                this.sendMessage(clientChannel, buf, m);
            }
            this.sendResponseToSuccessfulLogout(key, clientChannel);
            hm.put(Attachment.LOGOUT_SUCCESSFUL, Boolean.FALSE);
        }

        if (!attachment.isSendingPendingLock()) {
            SendPending pending = new SendPending(key, clientChannel);
            Thread t = new Thread(pending);
            t.start();
        }
    }

    private void registerClient(SelectionKey key, SocketChannel clientChannel) {
        try {
            clientChannel.configureBlocking(false); // Must be nonblocking to register
            // Register the selector with new channel for read and attach 
            //an attachment with a byte buffer an a clientId as a String
            com.orboan.chatapp.j.server.model.Attachment attachment
                    = new com.orboan.chatapp.j.server.model.Attachment();

            clientChannel.register(key.selector(),
                    SelectionKey.OP_READ,
                    attachment);
            key.attach(attachment);

        } catch (ClosedChannelException cce) {
            Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.SEVERE, null, cce);
        } catch (IOException ioe) {
            Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.SEVERE, null, ioe);
        }
    }

    private byte[] bytesToSend(Message message) {
        byte[] msg = SocketUtils.objectToBytes(message);
        int len = msg.length;
        byte[] lenBytes
                = {(byte) (len >> 24), (byte) (len >> 16), (byte) (len >> 8), (byte) (len)};
        return SocketUtils.concat(lenBytes, msg);
    }

    private void sendBroadcastMessage(ByteBuffer buf, Message message, SocketChannel from) {
        List<SocketChannel> failedDeliveryRecipientChannels = new ArrayList<>();
        this.prepareWriteBuffer(buf, message);
        for (Map.Entry entry : clients.entrySet()) {
            ArrayList<SocketChannel> clientChannels = (ArrayList<SocketChannel>) entry.getValue();
            for (SocketChannel clientChannel : clientChannels) {
                if (clientChannel != from) {
                    if (!this.writeToChannel(clientChannel, buf)) {
                        StringBuilder sb = new StringBuilder("failed to write to ");
                        String msg = sb.append("client channel with IP = '").
                                append(clientChannel.socket().getInetAddress().getHostAddress()).
                                append("' from server").toString();
                        Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.WARNING, msg);
                        failedDeliveryRecipientChannels.add(clientChannel);
                    }
                }
            }
        }
        if (!failedDeliveryRecipientChannels.isEmpty()) {
            for (SocketChannel recipientChannel : failedDeliveryRecipientChannels) {
                //sendMessage method signature:
                //private void sendMessage(SocketChannel clientChannel, ByteBuffer buf, Message message);  
                this.sendMessage(recipientChannel, buf, message);
            }
        }
    }

    private void sendMessage(SocketChannel clientChannel, ByteBuffer buf, Message message) {
        this.prepareWriteBuffer(buf, message);
        if (!this.writeToChannel(clientChannel, buf)) {
            StringBuilder sb = new StringBuilder("failed to write to ");
            String msg = sb.append("client channel with IP = '").
                    append(clientChannel.socket().getInetAddress().getHostAddress()).
                    append("' from server").toString();
            Logger.getLogger(ChatSelectorProtocol.class.getName()).log(Level.SEVERE, msg);
            this.saveMessageToMemory(message);//For trying later to send it again
        }
    }

    private void saveMessageToMemory(Message message) {
        ArrayList messagesList = this.messages.remove(message.getSender());
        if (messagesList == null) {
            messagesList = new ArrayList<>();
        }
        messagesList.add(message);
        this.messages.put(message.getSender(), messagesList);
    }

    private void prepareWriteBuffer(ByteBuffer buf, Message message) {
        buf.clear();
        buf.put(bytesToSend(message));
        buf.flip();
    }

    private boolean writeToChannel(SocketChannel clientChannel, ByteBuffer buf) {
        long nbytes = 0;
        long toWrite = buf.remaining();

        // loop on the channel.write() call since it will not necessarily
        // write all bytes in one shot
        try {
            while (nbytes != toWrite) {
                nbytes += clientChannel.write(buf);

                try {
                    Thread.sleep(ServerSettings.CHANNEL_WRITE_SLEEP);
                } catch (InterruptedException e) {
                    return false;
                }
            }
        } catch (ClosedChannelException cce) {
            return false;
        } catch (IOException e) {
            return false;
        }

        // get ready for another write if needed
        buf.rewind();
        return true;
    }

    class SendPending implements Runnable {

        private final SelectionKey key;
        private final SocketChannel clientChannel;

        public SendPending(SelectionKey key, SocketChannel clientChannel) {
            this.key = key;
            this.clientChannel = clientChannel;
        }

        @Override
        public void run() {
            Message message;
            com.orboan.chatapp.j.server.model.Attachment attachment
                    = (com.orboan.chatapp.j.server.model.Attachment) key.attachment();
            attachment.setSendingPendingLock(true);
            ByteBuffer buf = attachment.getByteBuf();
            if (attachment.getClientId() != null) {
                while (messages.get(attachment.getClientId()) != null) {
                    List<Message> thisClientMessages = messages.get(attachment.getClientId());
                    if (thisClientMessages != null) {
                        for (Message msg : thisClientMessages) {
                            if (msg.getRecipient().equals(Message.Recipient.ALL.toString())) {
                                sendBroadcastMessage(buf, msg, clientChannel);
                            } else {
                                ArrayList<SocketChannel> recipientChannels
                                        = clients.get(msg.getRecipient());
                                for (SocketChannel recipientChannel : recipientChannels) {
                                    sendMessage(recipientChannel, buf, msg);
                                }
                            }
                            buf.compact();
                            Logger.getLogger(SendPending.class.getName()).log(
                                    Level.INFO, "[Sender]{0} : [Content]{1} : [Recipient]{2}",
                                    new String[]{msg.getSender(),
                                        msg.getContent(),
                                        msg.getRecipient()});
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(SendPending.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        if (thisClientMessages.isEmpty()) {
                            messages.remove(attachment.getClientId());
                            attachment.setSendingPendingLock(false);
                        }
                    }
                }
                if (messages.get(attachment.getClientId()) == null) {
                    message = Messages.getSERVER_INFO_NO_MORE_MESSAGES();
                    Logger.getLogger(SendPending.class.getName()).log(
                            Level.INFO, "{0} : {1}", new String[]{message.getSender(), message.getContent()});
                }
            }
        }
    }
}
