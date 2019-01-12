package com.eatsleeppong.ubipong.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RatingAdjustmentRequest {
    private String tournamentName;
    private Date tournamentDate;

    private List<PlayerRatingLineItem> playerRatingList;
}
