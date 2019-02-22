package com.eatsleeppong.ubipong.model;

import lombok.Data;

import java.util.Date;

@Data
public class TournamentResultRequest {
    private String tournamentName;
    private Date tournamentDate;

    private TournamentResultLineItem[] tournamentResultList;
}
