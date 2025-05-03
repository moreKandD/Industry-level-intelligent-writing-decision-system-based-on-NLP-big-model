package com.textouput.backend.exception;

public class DeepSeekException extends Exception {
    public DeepSeekException(String message) {
        super(message);
    }

    public DeepSeekException(String message, Throwable cause) {
        super(message, cause);
    }
}