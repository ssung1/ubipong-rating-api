package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.controller.DuplicateTournamentException;
import com.eatsleeppong.ubipong.controller.RatingInputFormatException;
import com.eatsleeppong.ubipong.entity.MatchResult;
import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.Tournament;
import com.eatsleeppong.ubipong.model.*;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.repository.PlayerRepository;
import com.eatsleeppong.ubipong.repository.TournamentRepository;
import name.subroutine.etable.CsvTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

@Service
public class RatingManager {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private PlayerRepository playerRepository;
    private PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository;
    private TournamentRepository tournamentRepository;
    private RatingCalculator ratingCalculator;

    public RatingManager(
            PlayerRepository playerRepository,
            PlayerRatingAdjustmentRepository playerRatingAdjustmentRepository,
            TournamentRepository tournamentRepository,
            RatingCalculator ratingCalculator
    ) {
        this.playerRepository = playerRepository;
        this.playerRatingAdjustmentRepository = playerRatingAdjustmentRepository;
        this.tournamentRepository = tournamentRepository;
        this.ratingCalculator = ratingCalculator;
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

    public Integer getPlayerId(String search) {
        return getPlayer(search).map(Player::getPlayerId).orElse(null);
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

    public RatingAdjustmentResponse verifyRatingByCsv(String csv)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToPlayerRatingAdjustment(csv);
        final List<PlayerRatingLineItem> playerRatingList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<PlayerRatingLineItemResult> playerRatingResultList = new ArrayList<>(playerRatingList.size());

        final String tournamentName = ratingAdjustmentRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been recorded",
                tournamentName));
        }

        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());

        for (PlayerRatingLineItem playerRating : playerRatingList) {
            final PlayerRatingLineItemResult playerRatingResult = new PlayerRatingLineItemResult();
            playerRatingResult.setOriginalRequest(playerRating);

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = getPlayer(playerUserName);
            if (!player.isPresent()) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResult.REJECT_REASON_INVALID_PLAYER);
                playerRatingResultList.add(playerRatingResult);
                continue;
            }

            try {
                final Integer rating = Integer.parseInt(playerRating.getRating());
                if (rating < 0) {
                    playerRatingResult.setProcessed(false);
                    playerRatingResult.setRejectReason(PlayerRatingLineItemResult.REJECT_REASON_INVALID_RATING);
                    playerRatingResultList.add(playerRatingResult);
                }
            } catch (Exception ex) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResult.REJECT_REASON_INVALID_RATING);
                playerRatingResultList.add(playerRatingResult);
            }
        }

        result.setPlayerRatingResultList(playerRatingResultList);
        return result;
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
            throws IOException, RatingInputFormatException, DuplicateTournamentException {

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToPlayerRatingAdjustment(csv);
        final List<PlayerRatingLineItem> playerRatingList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<PlayerRatingLineItemResult> playerRatingResultList = new ArrayList<>(playerRatingList.size());

        final String tournamentName = ratingAdjustmentRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been recorded",
                    tournamentName));
        }

        Tournament tournament = new Tournament();
        tournament.setName(tournamentName);
        tournament.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        tournamentRepository.save(tournament);

        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());

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
            throws IOException, RatingInputFormatException, DuplicateTournamentException {
        if (autoAddPlayer) {
            return adjustRatingByCsvWithPlayerFinder(csv, this::getOrCreatePlayer);
        } else {
            return adjustRatingByCsvWithPlayerFinder(csv, this::getPlayer);
        }
    }

    public Tournament addTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Optional<Tournament> getTournamentById(Integer id) {
        return tournamentRepository.findById(id);
    }

    /**
     * @param search can be ID, username, or {firstname lastname}
     * @return
     */
    public Optional<Tournament> getTournament(String search) {
        return tournamentRepository.findByName(search);
    }

    public Optional<Tournament> getOrCreateTournament(String name) {
        final Optional<Tournament> existing = getTournament(name);
        if (existing.isPresent()) {
            return existing;
        }
        final Tournament newTournament = new Tournament();
        // the search term becomes username
        newTournament.setName(name);

        return Optional.of(tournamentRepository.save(newTournament));
    }

    /**
     * Generate a MatchResult object which contains the rating delta.  This requires a map of players and their
     * initial ratings, in the form of PlayerRatingAdjustment.
     *
     * @param playerRatingAdjustmentMap
     * @param tournamentResultLineItem
     * @return
     */
    public MatchResult generateMatchResult(
            Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap,
            TournamentResultLineItem tournamentResultLineItem) {
        final String winnerUserName = tournamentResultLineItem.getWinner();
        final String loserUserName = tournamentResultLineItem.getLoser();

        final PlayerRatingAdjustment winnerRating = playerRatingAdjustmentMap.get(winnerUserName);
        final PlayerRatingAdjustment loserRating = playerRatingAdjustmentMap.get(loserUserName);

//        final Optional<Player> winner = getPlayer(winnerUserName);
//        final Optional<Player> loser = getPlayer(loserUserName);
//
//        final Integer winnerRating = winner
//                .flatMap(p -> getRating(p.getPlayerId()))
//                .map(PlayerRatingAdjustment::getFinalRating).orElse(0);
//        final Integer loserRating = loser
//                .flatMap(p -> getRating(p.getPlayerId()))
//                .map(PlayerRatingAdjustment::getFinalRating).orElse(0);

        final Integer winnerRatingDelta = ratingCalculator.calculateWinnerDelta(
                winnerRating.getInitialRating(), loserRating.getInitialRating());

        MatchResult matchResult = new MatchResult();
        matchResult.setWinnerId(winnerRating.getPlayerId());
        matchResult.setLoserId(loserRating.getPlayerId());
        matchResult.setWinnerRatingDelta(winnerRatingDelta);

        return matchResult;
    }

    public PlayerRatingAdjustment generatePlayerRatingAdjustment(MatchResult matchResult) {
        return null;
    }
}
