package com.android.ex.camera2.exceptions;

public class TimeoutRuntimeException extends RuntimeException {
    public TimeoutRuntimeException(String message) {
        super(message);
    }

    public TimeoutRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
