package com.practice.diy.exception;

public abstract class BeansException extends NestedRuntimeException{
    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
