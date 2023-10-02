package com.practice.diy.exception;

public abstract class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
