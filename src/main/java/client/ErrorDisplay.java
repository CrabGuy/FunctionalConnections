package client;

import shared.ErrorCode;

public final class ErrorDisplay {
    private ErrorDisplay() {}

    public static String message(String errorCode) {
        if (errorCode == null) {
            return "";
        }
        try {
            return ErrorCode.valueOf(errorCode).getMessage();
        } catch (IllegalArgumentException e) {
            return errorCode;
        }
    }
}