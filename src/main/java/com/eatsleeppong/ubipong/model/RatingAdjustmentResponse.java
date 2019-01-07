package com.eatsleeppong.ubipong.model;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import lombok.Data;

import java.util.List;

@Data
public class RatingAdjustmentResponse {
    public static Integer RELECT_REASON_INVALID_PLAYER = 100;
    public static Integer RELECT_REASON_INVALID_RATING = 101;

    private RatingAdjustmentRequest ratingAdjustmentRequest;
    private PlayerRatingAdjustment playerRatingAdjustment;

    private Boolean processed;
    private Integer rejectReason;
}
