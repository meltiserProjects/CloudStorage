package ru.grigorev.common.utils;

import io.netty.channel.ChannelHandlerContext;
import ru.grigorev.common.ConnectionSingleton;
import ru.grigorev.common.Info;
import ru.grigorev.common.message.Message;
import ru.grigorev.common.message.MessageType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author Dmitriy Grigorev
 * works only with ConnectionSingleton or ChannelHandlerContext classes
 */
public class FileHandler {
    private FileOutputStream fos;
    private boolean isWriting;

    public FileHandler() {
    }

    public void continueWriting(Message message) throws IOException {
        byte[] byteArr = message.getByteArr();
        int currentPart = message.getCurrentPart();
        int partsCount = message.getPartsCount();
        fos.write(byteArr);
        fos.flush();

        if (currentPart == partsCount) {
            System.out.println("closing fileoutputstream");
            fos.flush();
            fos.close();
            fos = null;
            isWriting = false;
        }
    }

    public boolean isWriting() {
        return isWriting;
    }

    public void startWriting(Path path) throws IOException {
        this.fos = new FileOutputStream(path.toFile(), true);
        isWriting = true;
    }

    public boolean sendFile(Object out, Path file) throws IOException {
        if (Files.exists(file) && !Files.isDirectory(file)) {
            if (Files.size(file) <= Info.MAX_FILE_SIZE) {
                System.out.println("file is less than max allowed size");
                sendSmallFile(out, file);
            } else {
                System.out.println("sending big file");
                sendBigFile(out, file);
            }
            return true;
        } else
            return false;
    }

    private void sendSmallFile(Object out, Path file) throws IOException {
        Message message = new Message(MessageType.FILE, file);
        sendMessageDependingOnInstance(out,message);
    }

    public void sendBigFile(Object out, Path path) throws IOException {
        long fileSize = Files.size(path);
        int partsCount = (int) (fileSize / Info.MAX_FILE_SIZE);
        if (fileSize % Info.MAX_FILE_SIZE != 0) partsCount++;

        System.out.println("total parts: " + partsCount);

        final int BUFFER_SIZE = Info.MAX_FILE_SIZE;
        FileInputStream is = new FileInputStream(path.toFile());
        byte[] buf = new byte[BUFFER_SIZE];

        int read = 0;
        int currentPartSending = 1;
        while ((read = is.read(buf)) > 0) {
            System.out.println("bufsize: " + read + " current part: " + currentPartSending);
            Message message;
            if (read < BUFFER_SIZE) {
                byte[] bytesLeft = Arrays.copyOfRange(buf, 0, read);
                message = new Message(MessageType.FILE_PART, bytesLeft, partsCount, currentPartSending, path.getFileName().toString());
            } else {
                message = new Message(MessageType.FILE_PART, buf, partsCount, currentPartSending, path.getFileName().toString());
            }
            sendMessageDependingOnInstance(out, message);
            currentPartSending++;
        }
        is.close();
        is = null;
    }

    private void sendMessageDependingOnInstance(Object out, Message message) {
        if (out instanceof ChannelHandlerContext) ((ChannelHandlerContext) out).writeAndFlush(message);
        if (out instanceof ConnectionSingleton) ((ConnectionSingleton) out).sendMessage(message);
    }

    /**
     * @return true if exist
     */
    public boolean checkFolderExisting(String path) {
        Path folder = Paths.get(path);
        if (Files.notExists(folder)) {
            try {
                System.out.println("Directory doesn't exist. Creating directory");
                Files.createDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
}
