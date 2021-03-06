package com.eatsleeppong.ubipong.rating.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RatingAdjustmentResponse {
    public static Integer REJECT_REASON_INVALID_TOURNAMENT_NAME = 450;
    public static Integer REJECT_REASON_INVALID_TOURNAMENT_DATE = 451;
    public static Integer REJECT_REASON_INVALID_RATING_HEADER = 452;

    private Integer tournamentId;
    private String tournamentName;
    private Date tournamentDate;

    private List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseList;

    private Boolean processed;

    public boolean isProcessed() {
        if (processed == null) {
            return false;
        } else {
            return processed;
        }
    }
}
