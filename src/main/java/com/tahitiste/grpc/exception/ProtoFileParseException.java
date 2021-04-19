package com.tahitiste.grpc.exception;

public class ProtoFileParseException extends RuntimeException{
    public ProtoFileParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtoFileParseException(Throwable cause) {
        super(cause);
    }

    public ProtoFileParseException(String message) {
        super(message);
    }
}
