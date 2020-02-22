package com.tuiasi.exception;


public class ObjectNotFoundException extends RuntimeException  {
    public ObjectNotFoundException() {
    }

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public ObjectNotFoundException(Throwable cause) {
        super(cause);
    }
}
