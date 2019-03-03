package com.eatsleeppong.ubipong.rating.controller;

public class DuplicateTournamentException extends Exception {
    public DuplicateTournamentException() {
    }
    public DuplicateTournamentException(String message) {
        super(message);
    }
    public DuplicateTournamentException(String message, Throwable cause) {
        super(message, cause);
    }
}
