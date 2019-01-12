package com.eatsleeppong.ubipong.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RatingAdjustmentResponse {
    private Integer tournamentId;
    private String tournamentName;
    private Date tournamentDate;

    private List<PlayerRatingLineItemResult> playerRatingResultList;
}
