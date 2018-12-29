package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.model.MatchResult;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.repository.PlayerRatingAdjustmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RatingManager {
    private PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository;

    public RatingManager(
        PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository
    ) {
        this.playerRatingAdjustmentRepository = playerRatingAdjustmentRepository;
    }

    private Map<Integer, List<PlayerRatingAdjustment>> ratingHistoryMap = new HashMap<>();

    public Optional<PlayerRatingAdjustment> getRating(Integer playerId) {
       Page<PlayerRatingAdjustment> ratingHistory =
           playerRatingAdjustmentRepository.findByPlayerId(playerId,
                   PageRequest.of(0, 1, Sort.Direction.DESC, "adjustmentDate"));

       return ratingHistory.get().findFirst();
    }

    public void processMatchResult(MatchResult matchResult) {
        return;
    }

    public PlayerRatingAdjustment adjustRating(PlayerRatingAdjustment playerRatingAdjustment) {
        return playerRatingAdjustmentRepository.save(playerRatingAdjustment);
    }
}
