package com.eatsleeppong.ubipong.entity;

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
    private Integer player1Id;
    private Integer player2Id;
    private Boolean winForPlayer1;

    public boolean isWinForPlayer1() {
        if (winForPlayer1 != null) {
            return winForPlayer1;
        } else {
            return false;
        }
    }
}
