package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.model.MatchResult;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.model.PlayerRatingLineItem;
import com.eatsleeppong.ubipong.model.RatingAdjustmentRequest;
import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;
import com.eatsleeppong.ubipong.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.repository.PlayerRepository;
import name.subroutine.etable.CsvTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;

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

    public Optional<Player> getPlayerById(Integer id) {
        return playerRepository.findById(id);
    }

    /**
     * @param search can be ID, username, or {firstname lastname}
     * @return
     */
    public Optional<Player> getPlayer(String search) {
        return playerRepository.findByUserName(search);
    }

    public Optional<Player> getOrCreatePlayer(String search) {
        final Optional<Player> existing = getPlayer(search);
        if (existing.isPresent()) {
            return existing;
        }
        final Player newUser = new Player();
        // the search term becomes username
        newUser.setUserName(search);

        return Optional.of(playerRepository.save(newUser));
    }

    public RatingAdjustmentResponse verifyRatingByCsv(String csv) throws IOException {
        return null;
    }

    /**
     * @param csv has two columns
     *     <pre>
     *     line 1                tournamentId         , {tournament ID (optional)}
     *     line 2                date                 , {date, in ISO8601 format}
     *     line 3                player               , rating
     *     line 4                {player1_username}   , {player1_rating}
     *     line 5                {player2_username}   , {player2_rating}
     *     line 6                {player3_username}   , {player3_rating}
     *     </pre>
     *
     * @return
     * @throws IOException
     */
    public RatingAdjustmentRequest convertCsvToPlayerRatingAdjustment(final String csv) throws IOException {
        final List<PlayerRatingLineItem> playerRatingLineItemList = new ArrayList<>();
        try(
                final StringReader sr = new StringReader(csv);
                final BufferedReader br = new BufferedReader(sr)
        ) {
            while(true) {
                final PlayerRatingLineItem playerRating = new PlayerRatingLineItem();
                final String line = br.readLine();
                if(line == null) break;

                final String[] record = CsvTable.toArray(line);

                try {
                    if (record.length > 0) {
                        playerRating.setPlayerUserName(record[0].trim());
                    }
                    if (record.length > 1) {
                        playerRating.setRating(record[1].trim());
                    }

                    playerRatingLineItemList.add(playerRating);
                } catch (Exception ex) {
                    // can't do much
                }
            }
        }

        final RatingAdjustmentRequest result = new RatingAdjustmentRequest();
        result.setPlayerRatingList(playerRatingLineItemList);
        return result;
    }

    private List<RatingAdjustmentResponse> adjustRatingByCsvWithPlayerFinder(
            final String csv,
            final Function<String, Optional<Player>> playerFinder) throws IOException {

        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToPlayerRatingAdjustment(csv);
        final List<PlayerRatingLineItem> playerRatingLineItemList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<RatingAdjustmentResponse> result = new ArrayList<>(playerRatingLineItemList.size());

        for (PlayerRatingLineItem playerRating : playerRatingLineItemList) {
            final RatingAdjustmentResponse ratingAdjustmentResponse = new RatingAdjustmentResponse();
            result.add(ratingAdjustmentResponse);
            // TODO: ratingAdjustmentResponse.setRatingAdjustmentRequest(playerRating);

            final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = playerFinder.apply(playerUserName);
            if (player.isPresent()) {
                playerRatingAdjustment.setPlayerId(player.get().getPlayerId());
            } else {
                ratingAdjustmentResponse.setProcessed(false);
                ratingAdjustmentResponse.setRejectReason(RatingAdjustmentResponse.RELECT_REASON_INVALID_PLAYER);
                continue;
            }

            try {
                final Integer rating = Integer.parseInt(playerRating.getRating());
                final Integer prevRating = getRating(player.get().getPlayerId())
                        .map(PlayerRatingAdjustment::getFinalRating)
                        .orElse(0);
                playerRatingAdjustment.setInitialRating(prevRating);
                playerRatingAdjustment.setFirstPassRating(prevRating);
                playerRatingAdjustment.setFinalRating(rating);
                playerRatingAdjustment.setAdjustmentDate(new Date());
            } catch (Exception ex) {
                ratingAdjustmentResponse.setProcessed(false);
                ratingAdjustmentResponse.setRejectReason(RatingAdjustmentResponse.RELECT_REASON_INVALID_RATING);
                continue;
            }

            playerRatingAdjustmentRepository.save(playerRatingAdjustment);
            ratingAdjustmentResponse.setPlayerRatingAdjustment(playerRatingAdjustment);
            ratingAdjustmentResponse.setProcessed(true);
        }

        return result;
    }

    public List<RatingAdjustmentResponse> adjustRatingByCsv(final String csv, boolean autoAddPlayer) throws IOException {
        if (autoAddPlayer) {
            return adjustRatingByCsvWithPlayerFinder(csv, (username) -> {
                final Optional<Player> existing = playerRepository.findByUserName(username);
                if (existing.isPresent()) return existing;

                final Player newUser = new Player();
                newUser.setUserName(username);

                return Optional.of(playerRepository.save(newUser));
            });
        } else {
            return adjustRatingByCsvWithPlayerFinder(csv, playerRepository::findByUserName);
        }
    }
}
