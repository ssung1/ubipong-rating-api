package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_result_seq")
    private Integer matchResultId;

    private Integer tournamentId;
    private String eventName;
    private Integer winnerId;
    private Integer loserId;
    private String resultString;
    private Integer winnerRatingDelta;
}
