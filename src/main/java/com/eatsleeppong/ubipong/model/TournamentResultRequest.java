package com.eatsleeppong.ubipong.model;

import lombok.Data;

@Data
public class TournamentResultRequest {
    private TournamentResultLineItem[] tournamentResultLineItem;
}
