package ru.grigorev.common;

/**
 * @author Dmitriy Grigorev
 */
public class Info {
    public static final int PORT = 8189;
    public static final String HOST = "localhost";
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/Users";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "postgres";
    public static final String CLIENT_FOLDER_NAME = "client_storage\\";
    public static final String SERVER_FOLDER_NAME = "server_storage\\";
    public static final int MAX_FILE_SIZE = 1024 * 1024 * 16; // 16 mb
}
