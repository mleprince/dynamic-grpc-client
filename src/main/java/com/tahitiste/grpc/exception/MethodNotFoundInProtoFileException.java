package com.tahitiste.grpc.exception;

public class MethodNotFoundInProtoFileException extends RuntimeException{
    public MethodNotFoundInProtoFileException(String message) {
        super(message);
    }
}
