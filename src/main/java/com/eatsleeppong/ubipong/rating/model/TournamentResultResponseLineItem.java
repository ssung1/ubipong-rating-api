package com.eatsleeppong.ubipong.rating.model;

import com.eatsleeppong.ubipong.rating.entity.MatchResult;
import lombok.Data;

/**
 * This is a response to each TournamentResultRequestLineItem.  It includes the original line item plus the MatchResult
 * which contains the rating delta
 */
@Data
public class TournamentResultResponseLineItem {
    public static Integer REJECT_REASON_INVALID_WINNER = 400;
    public static Integer REJECT_REASON_INVALID_LOSER = 401;

    private TournamentResultRequestLineItem originalTournamentResultLineItem;
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
