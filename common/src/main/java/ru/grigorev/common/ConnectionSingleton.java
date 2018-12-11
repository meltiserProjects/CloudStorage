package ru.grigorev.common;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import ru.grigorev.common.message.AuthMessage;
import ru.grigorev.common.message.Message;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Dmitriy Grigorev
 */
public class ConnectionSingleton {
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;
    private Socket socket;

    private static ConnectionSingleton ourInstance = new ConnectionSingleton();

    private ConnectionSingleton() {
    }

    public static ConnectionSingleton getInstance() {
        return ourInstance;
    }

    public void init() {
        try {
            socket = new Socket(Info.HOST, Info.PORT);
            in = new ObjectDecoderInputStream(socket.getInputStream(), 1024 * 1024 * 100);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAuthMessage(AuthMessage authMessage) {
        try {
            out.writeObject(authMessage);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object receiveMessage() {
        Object received = null;
        try {
            received = in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return received;
    }
}
