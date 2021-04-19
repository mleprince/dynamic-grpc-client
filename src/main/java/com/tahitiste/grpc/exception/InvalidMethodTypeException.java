package com.tahitiste.grpc.exception;

public class InvalidMethodTypeException extends RuntimeException {
    public InvalidMethodTypeException(String methodName, String message) {
        super("[method:" + methodName + "] " + message);
    }
}
