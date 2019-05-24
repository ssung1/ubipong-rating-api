package com.eatsleeppong.ubipong.rating.controller;

public class DuplicateTournamentException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateTournamentException() {
    }
    public DuplicateTournamentException(String message) {
        super(message);
    }
    public DuplicateTournamentException(String message, Throwable cause) {
        super(message, cause);
    }
}
