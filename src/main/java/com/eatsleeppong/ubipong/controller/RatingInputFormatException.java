package com.eatsleeppong.ubipong.controller;

import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;

import java.text.ParseException;

public class RatingInputFormatException extends Exception {
    public RatingInputFormatException() {
    }
    public RatingInputFormatException(String message) {
        super(message);
    }
    public RatingInputFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
