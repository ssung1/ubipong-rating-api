package com.eatsleeppong.ubipong.rating.model;

import com.eatsleeppong.ubipong.rating.entity.PlayerRatingAdjustment;
import lombok.Data;

/**
 * After a RatingAdjustmentRequestLineItem is processed, we return this result, which includes the original
 * RatingAdjustmentRequestLineItem plus any error codes if the request could not be processed.
 */
@Data
public class RatingAdjustmentResponseLineItem {
    public static Integer REJECT_REASON_INVALID_PLAYER = 400;
    public static Integer REJECT_REASON_INVALID_RATING = 401;

    private RatingAdjustmentRequestLineItem originalRequest;
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
