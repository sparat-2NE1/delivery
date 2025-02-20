package com.sparta.delivery.config.global.exception.custom;

public class RefreshTokenAlreadyExistsException extends RuntimeException{
    public RefreshTokenAlreadyExistsException(String message) {
        super(message);
    }
}
