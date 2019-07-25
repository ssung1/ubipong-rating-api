package com.eatsleeppong.ubipong.rating.controller;

public class RatingInputFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    public RatingInputFormatException() {
    }
    public RatingInputFormatException(String message) {
        super(message);
    }
    public RatingInputFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
