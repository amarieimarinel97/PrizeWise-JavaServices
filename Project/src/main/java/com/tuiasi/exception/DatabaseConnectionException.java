package com.tuiasi.exception;

public class DatabaseConnectionException extends RuntimeException {
    public DatabaseConnectionException() {
    }

    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(Throwable cause) {
        super(cause);
    }
}
