package com.eatsleeppong.ubipong.model;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import lombok.Data;

import java.util.List;

@Data
public class PlayerRatingLineItemResult {
    public static Integer REJECT_REASON_INVALID_PLAYER = 400;
    public static Integer REJECT_REASON_INVALID_RATING = 401;

    private PlayerRatingLineItem originalRequest;
    private PlayerRatingAdjustment adjustmentResult;

    private Boolean processed;
    private Integer rejectReason;

    public boolean isProcessed() {
        if (processed == null) {
            return false;
        } else {
            return processed;
        }
    }
}
