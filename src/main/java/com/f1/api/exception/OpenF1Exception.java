package com.f1.api.exception;

import lombok.Getter;

@Getter
public class OpenF1Exception extends RuntimeException {

    private final int statusCode;

    public OpenF1Exception(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

}
