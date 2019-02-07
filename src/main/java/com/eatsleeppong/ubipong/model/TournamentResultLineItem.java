package com.eatsleeppong.ubipong.model;

import lombok.Data;

@Data
public class TournamentResultLineItem {
    private String winner;
    private String loser;

    private String resultString;
}
