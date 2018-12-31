package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.model.MatchResult;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.repository.PlayerRepository;
import name.subroutine.etable.CsvTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@Service
public class RatingManager {
    private PlayerRepository playerRepository;
    private PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository;

    public RatingManager(
            PlayerRepository playerRepository,
            PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository
    ) {
        this.playerRepository = playerRepository;
        this.playerRatingAdjustmentRepository = playerRatingAdjustmentRepository;
    }

    public Optional<PlayerRatingAdjustment> getRating(Integer playerId) {
       Page<PlayerRatingAdjustment> ratingHistory =
           playerRatingAdjustmentRepository.findByPlayerId(playerId,
                   PageRequest.of(0, 1, Sort.Direction.DESC, "adjustmentDate"));

       return ratingHistory.get().findFirst();
    }

    public List<PlayerRatingAdjustment> getRatingHistory(Integer playerId, int size) {
        Page<PlayerRatingAdjustment> ratingHistory =
            playerRatingAdjustmentRepository.findByPlayerId(playerId,
                PageRequest.of(0, size, Sort.Direction.DESC, "adjustmentDate"));

        return ratingHistory.getContent();
    }
    public void processMatchResult(MatchResult matchResult) {
        return;
    }

    public PlayerRatingAdjustment adjustRating(PlayerRatingAdjustment playerRatingAdjustment) {
        return playerRatingAdjustmentRepository.save(playerRatingAdjustment);
    }

    public Player addPlayer(Player player) {
        return playerRepository.save(player);
    }

    public Player getPlayerById(Integer id) {
        return null;
    }

    /**
     * @param search can be ID, username, or {firstname lastname}
     * @return
     */
    public Player getPlayer(String search) {
        return playerRepository.findByUserName(search);
    }

    public void adjustRatingByCsv(String csv) throws IOException {
        try(
            StringReader sr = new StringReader(csv);
            BufferedReader br = new BufferedReader(sr)
        ) {
            while(true) {
                final String line = br.readLine();
                if(line == null) break;

                final String[] record = CsvTable.toArray(line);
                if(record.length < 2) continue;

                final String playerUserName = record[0];
                final Player player = playerRepository.findByUserName(playerUserName);

                if(player == null) continue;

                final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();
                Integer rating = Integer.parseInt(record[1].trim());
                playerRatingAdjustment.setPlayerId(player.getPlayerId());

                final Integer prevRating = getRating(player.getPlayerId()).map(PlayerRatingAdjustment::getFinalRating)
                        .orElse(0);
                playerRatingAdjustment.setInitialRating(prevRating);
                playerRatingAdjustment.setFirstPassRating(prevRating);
                playerRatingAdjustment.setFinalRating(rating);
                playerRatingAdjustment.setAdjustmentDate(new Date());

                playerRatingAdjustmentRepository.save(playerRatingAdjustment);
            }
        }
    }
}
