package com.eatsleeppong.ubipong.model;

import com.eatsleeppong.ubipong.entity.MatchResult;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import lombok.Data;

/**
 * This is a response to each TournamentResultLineItem.  It includes the original line item plus the MatchResult
 * which contains the rating delta
 */
@Data
public class TournamentResultLineItemResponse {
    public static Integer REJECT_REASON_INVALID_PLAYER = 400;
    public static Integer REJECT_REASON_INVALID_RATING = 401;

    private TournamentResultLineItem originalTournamentResultLineItem;
    private MatchResult matchResult;

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
