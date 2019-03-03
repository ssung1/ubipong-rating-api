package com.eatsleeppong.ubipong.rating.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * A request to adjust a players' ratings after a tournament.
 */
@Data
public class RatingAdjustmentRequest {
    private String tournamentName;
    private Date tournamentDate;

    private List<PlayerRatingLineItem> playerRatingList;
}
