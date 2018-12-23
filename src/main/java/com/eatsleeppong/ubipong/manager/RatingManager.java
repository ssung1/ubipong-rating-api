package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.model.MatchResult;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatingManager {
    private Map<Integer, List<PlayerRatingAdjustment>> ratingHistoryMap = new HashMap<>();

    public PlayerRatingAdjustment getRating(Integer playerId) {
        final List<PlayerRatingAdjustment> ratingHistory = ratingHistoryMap.get(playerId);
        final int size = ratingHistory.size();
        return ratingHistory.get(size - 1);
    }

    public void processMatchResult(MatchResult matchResult) {
        return;
    }

    public void adjustRating(PlayerRatingAdjustment playerRatingAdjustment) {
        final Integer playerId = playerRatingAdjustment.getPlayerRatingAdjustmentId();
        List<PlayerRatingAdjustment> ratingHistory = ratingHistoryMap.get(playerId);

        if (ratingHistory == null) {
            ratingHistory = new ArrayList<>();
            ratingHistoryMap.put(playerId, ratingHistory);
        }

        ratingHistory.add(playerRatingAdjustment);
    }
}
