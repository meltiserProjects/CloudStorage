package ru.grigorev.common.message;

/**
 * @author Dmitriy Grigorev
 */
public enum MessageType {
    FILE,
    ABOUT_FILE,
    FIlE_RENAME,
    FILE_REQUEST,
    REFRESH_REQUEST,
    REFRESH_RESPONSE,
    DELETE_FILE,
    FILE_PART,
    SIGN_IN_REQUEST,
    SIGN_UP_REQUEST,
    SIGN_OUT_REQUEST,
    SIGN_OUT_RESPONSE,
    DISCONNECTING,
    AUTH_OK,
    AUTH_FAIL
}
