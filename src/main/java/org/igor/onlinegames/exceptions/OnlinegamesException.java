package org.igor.onlinegames.exceptions;

public class OnlinegamesException extends RuntimeException {
    public OnlinegamesException(String message) {
        super(message);
    }

    public OnlinegamesException(Throwable cause) {
        super(cause);
    }
}
