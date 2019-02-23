package com.eatsleeppong.ubipong.model;

import lombok.Data;

/**
 * This is a match result, but we are calling it TournamentResultLineItem to avoid confusion with the entity
 * actually called MatchResult
 */
@Data
public class TournamentResultLineItem {
    private String winner;
    private String loser;

    private String resultString;
}
