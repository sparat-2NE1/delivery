package com.sparta.delivery.config.global.exception.custom;

public class InvalidTokenException extends RuntimeException{
    public InvalidTokenException(String message) {
        super(message);
    }
}
