package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.controller.RatingInputFormatException;
import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.model.*;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.repository.PlayerRepository;
import name.subroutine.etable.CsvTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class RatingManager {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

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

    public PlayerRatingLineItemResult verifyRatingByCsv(String csv) throws IOException {
        return null;
    }

    /**
     * called exclusively by convertCsvToPlayerRatingAdjustment to process a line of header
     *
     * @param line one line from the CSV
     * @param lineNumber line number (for better error reporting)
     * @param contentName what we are expecting in that line
     * @param action what we want to do with the content
     */
    private void processHeaderLine(final String line, final int lineNumber, final String contentName, Consumer<String> action)
            throws RatingInputFormatException {
        if (line == null) {
            throw new RatingInputFormatException(MessageFormat.format("Missing line {0}", lineNumber));
        }
        try {
            final String[] values = CsvTable.toArray(line);
            action.accept(values[1].trim());
        } catch (ArrayIndexOutOfBoundsException outOfBounds) {
            throw new RatingInputFormatException(MessageFormat.format(
                    "Missing {0} on line {1}: {2}", contentName, lineNumber, line));
        } catch (Exception ex) {
            throw new RatingInputFormatException(MessageFormat.format(
                    "Could not understand {0} on line {1}: {2}", contentName, lineNumber, line));
        }
    }

    /**
     * @param csv has two columns
     *     <pre>
     *     line 1                tournamentName       , {tournament name}
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
    public RatingAdjustmentRequest convertCsvToPlayerRatingAdjustment(final String csv)
            throws IOException, RatingInputFormatException {
        final RatingAdjustmentRequest result = new RatingAdjustmentRequest();
        final List<PlayerRatingLineItem> playerRatingLineItemList = new ArrayList<>();
        try(
                final StringReader sr = new StringReader(csv);
                final BufferedReader br = new BufferedReader(sr)
        ) {
            processHeaderLine(br.readLine(), 1, "tournament name", result::setTournamentName);
            processHeaderLine(br.readLine(), 2, "tournament date", (s) -> {
                try {
                    result.setTournamentDate(df.parse(s));
                } catch (ParseException ex) {
                    throw new RuntimeException(ex);
                }
            });
            processHeaderLine(br.readLine(), 3, "player rating header", (s) -> {
                if (!s.equals("rating")) {
                    throw new RuntimeException();
                }
            });

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

        result.setPlayerRatingList(playerRatingLineItemList);
        return result;
    }

    private RatingAdjustmentResponse adjustRatingByCsvWithPlayerFinder(
            final String csv,
            final Function<String, Optional<Player>> playerFinder)
            throws IOException, RatingInputFormatException {

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToPlayerRatingAdjustment(csv);
        final List<PlayerRatingLineItem> playerRatingList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<PlayerRatingLineItemResult> playerRatingResultList = new ArrayList<>(playerRatingList.size());

        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());
        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());

        for (PlayerRatingLineItem playerRating : playerRatingList) {
            final PlayerRatingLineItemResult playerRatingResult = new PlayerRatingLineItemResult();
            playerRatingResultList.add(playerRatingResult);
            playerRatingResult.setOriginalRequest(playerRating);

            final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = playerFinder.apply(playerUserName);
            if (player.isPresent()) {
                playerRatingAdjustment.setPlayerId(player.get().getPlayerId());
            } else {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResult.REJECT_REASON_INVALID_PLAYER);
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
                playerRatingAdjustment.setAdjustmentDate(ratingAdjustmentRequest.getTournamentDate());
            } catch (Exception ex) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResult.REJECT_REASON_INVALID_RATING);
                continue;
            }

            playerRatingAdjustmentRepository.save(playerRatingAdjustment);
            playerRatingResult.setAdjustmentResult(playerRatingAdjustment);
            playerRatingResult.setProcessed(true);
        }

        result.setPlayerRatingResultList(playerRatingResultList);
        return result;
    }

    public RatingAdjustmentResponse adjustRatingByCsv(final String csv, boolean autoAddPlayer)
            throws IOException, RatingInputFormatException {
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
