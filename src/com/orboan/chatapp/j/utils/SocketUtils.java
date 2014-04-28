/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.orboan.chatapp.j.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oriol Boix Anfosso <dev@orboan.com>
 */
public final class SocketUtils {

    public static byte[] objectToBytes(Object obj) {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);) {
            out.writeObject(obj);
            bytes = bos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(SocketUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bytes;
    }

    public static Object bytesToObject(byte[] bytes) {
        Object o = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = new ObjectInputStream(bis);) {
            o = in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(SocketUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return o;
    }

    public static <T> T[] concat(T[] A, T[] B) {
        int aLen = A.length;
        int bLen = B.length;

        @SuppressWarnings("unchecked")
        T[] C = (T[]) Array.newInstance(A.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);

        return C;
    }

    public static byte[] concat(byte[] A, byte[] B) {
        int aLen = A.length;
        int bLen = B.length;
        byte[] C = new byte[aLen + bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }
}
