package com.eatsleeppong.ubipong.model;

import lombok.Data;

@Data
public class MatchResult {
    private Integer player1;
    private Integer player2;
    private boolean winForPlayer1;
}
