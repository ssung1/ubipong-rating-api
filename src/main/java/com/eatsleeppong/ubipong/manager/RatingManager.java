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
import org.springframework.transaction.annotation.Transactional;

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
import java.util.stream.Collectors;

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

    public PlayerRatingAdjustment addPlayerRatingAdjustment(PlayerRatingAdjustment playerRatingAdjustment) {
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
    public Optional<Player> getPlayer(final String search) {
        return playerRepository.findByUserName(search);
    }

    public Integer getPlayerId(final String search) {
        return getPlayer(search).map(Player::getPlayerId).orElse(null);
    }

    public Optional<Player> getOrCreatePlayer(final String search) {
        final Optional<Player> existing = getPlayer(search);
        if (existing.isPresent()) {
            return existing;
        }
        final Player newUser = new Player();
        // the search term becomes username
        newUser.setUserName(search);

        return Optional.of(playerRepository.save(newUser));
    }

    public RatingAdjustmentResponse verifyRatingByCsv(final String csv)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToRatingAdjustmentRequest(csv);
        final List<PlayerRatingLineItem> playerRatingList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<PlayerRatingLineItemResponse> playerRatingLineItemResponseList =
                new ArrayList<>(playerRatingList.size());

        final String tournamentName = ratingAdjustmentRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been recorded",
                tournamentName));
        }

        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());

        for (PlayerRatingLineItem playerRating : playerRatingList) {
            final PlayerRatingLineItemResponse playerRatingResult = new PlayerRatingLineItemResponse();
            playerRatingResult.setOriginalRequest(playerRating);

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = getPlayer(playerUserName);
            if (!player.isPresent()) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResponse.REJECT_REASON_INVALID_PLAYER);
                playerRatingLineItemResponseList.add(playerRatingResult);
                continue;
            }

            try {
                final Integer rating = Integer.parseInt(playerRating.getRating());
                if (rating < 0) {
                    playerRatingResult.setProcessed(false);
                    playerRatingResult.setRejectReason(PlayerRatingLineItemResponse.REJECT_REASON_INVALID_RATING);
                    playerRatingLineItemResponseList.add(playerRatingResult);
                }
            } catch (Exception ex) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(PlayerRatingLineItemResponse.REJECT_REASON_INVALID_RATING);
                playerRatingLineItemResponseList.add(playerRatingResult);
            }
        }

        result.setPlayerRatingList(playerRatingLineItemResponseList);
        return result;
    }

    /**
     * called exclusively by convertCsvToRatingAdjustmentRequest to process a line of header
     *
     * @param line one line from the CSV
     * @param lineNumber line number (for better error reporting)
     * @param contentName what we are expecting in that line
     * @param action what we want to do with the content
     */
    private void processHeaderLine(final String line, final int lineNumber, final String contentName,
            final Consumer<String> action)
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
    private RatingAdjustmentRequest convertCsvToRatingAdjustmentRequest(final String csv)
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

    @Transactional
    public RatingAdjustmentResponse adjustRatingByCsv(final String csv, final boolean autoAddPlayer)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToRatingAdjustmentRequest(csv);
        return adjustRating(ratingAdjustmentRequest, autoAddPlayer);
    }

    private RatingAdjustmentResponse adjustRatingWithPlayerFinder(
            final RatingAdjustmentRequest ratingAdjustmentRequest,
            final Function<String, Optional<Player>> playerFinder)
            throws DuplicateTournamentException {

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        final List<PlayerRatingLineItem> playerRatingList = ratingAdjustmentRequest.getPlayerRatingList();
        final List<PlayerRatingLineItemResponse> playerRatingLineItemResponseList =
                new ArrayList<>(playerRatingList.size());

        final String tournamentName = ratingAdjustmentRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been recorded",
                    tournamentName));
        }

        final Tournament tournament = new Tournament();
        tournament.setName(tournamentName);
        tournament.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        final Tournament savedTournament = tournamentRepository.save(tournament);

        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());
        result.setTournamentId(savedTournament.getTournamentId());

        for (PlayerRatingLineItem playerRating : playerRatingList) {
            final PlayerRatingLineItemResponse playerRatingLineItemResponse = new PlayerRatingLineItemResponse();
            playerRatingLineItemResponseList.add(playerRatingLineItemResponse);
            playerRatingLineItemResponse.setOriginalRequest(playerRating);

            final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = playerFinder.apply(playerUserName);
            if (player.isPresent()) {
                playerRatingAdjustment.setPlayerId(player.get().getPlayerId());
            } else {
                playerRatingLineItemResponse.setProcessed(false);
                playerRatingLineItemResponse.setRejectReason(PlayerRatingLineItemResponse.REJECT_REASON_INVALID_PLAYER);
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
                playerRatingLineItemResponse.setProcessed(false);
                playerRatingLineItemResponse.setRejectReason(PlayerRatingLineItemResponse.REJECT_REASON_INVALID_RATING);
                continue;
            }

            playerRatingAdjustmentRepository.save(playerRatingAdjustment);
            playerRatingLineItemResponse.setAdjustmentResult(playerRatingAdjustment);
            playerRatingLineItemResponse.setProcessed(true);
        }

        result.setPlayerRatingList(playerRatingLineItemResponseList);
        return result;
    }

    public RatingAdjustmentResponse adjustRating(
            final RatingAdjustmentRequest ratingAdjustmentRequest, final boolean autoAddPlayer)
            throws DuplicateTournamentException {
        if (autoAddPlayer) {
            return adjustRatingWithPlayerFinder(ratingAdjustmentRequest, this::getOrCreatePlayer);
        } else {
            return adjustRatingWithPlayerFinder(ratingAdjustmentRequest, this::getPlayer);
        }
    }

    public Tournament addTournament(final Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Optional<Tournament> getTournamentById(final Integer id) {
        return tournamentRepository.findById(id);
    }

    /**
     * @param search can be ID, username, or {firstname lastname}
     * @return
     */
    public Optional<Tournament> getTournament(final String search) {
        return tournamentRepository.findByName(search);
    }

    public Optional<Tournament> getOrCreateTournament(final String name) {
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
     * final ratings (from their latest adjustment), in the form of PlayerRatingAdjustment.
     *
     * @param playerRatingAdjustmentMap
     * @param tournamentResultLineItem
     * @return
     */
    public MatchResult generateMatchResult(
            final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap,
            final TournamentResultLineItem tournamentResultLineItem) {
        final String winnerUserName = tournamentResultLineItem.getWinner();
        final String loserUserName = tournamentResultLineItem.getLoser();

        final PlayerRatingAdjustment winnerRating = playerRatingAdjustmentMap.get(winnerUserName);
        final PlayerRatingAdjustment loserRating = playerRatingAdjustmentMap.get(loserUserName);

        final Integer winnerRatingDelta = ratingCalculator.calculateWinnerDelta(
                winnerRating.getFinalRating(), loserRating.getFinalRating());

        MatchResult matchResult = new MatchResult();
        matchResult.setWinnerId(winnerRating.getPlayerId());
        matchResult.setLoserId(loserRating.getPlayerId());
        matchResult.setWinnerRatingDelta(winnerRatingDelta);

        return matchResult;
    }

    /**
     * @param playerRatingAdjustmentMap
     * @param matchResultList
     * @return a new map of ratings.  it uses player Id as key because it is only used internally for processing
     */
    public Map<Integer, PlayerRatingAdjustment> applyMatchResultList(
            final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap,
            final List<MatchResult> matchResultList) {
        final Map<Integer, PlayerRatingAdjustment> result = new HashMap<>();

        // make a copy of the PlayerRatingAdjustment map
        playerRatingAdjustmentMap.values().forEach((v) -> {
            PlayerRatingAdjustment newRating;
            try {
                newRating = (PlayerRatingAdjustment)v.clone();
            } catch (CloneNotSupportedException ex) {
                newRating = new PlayerRatingAdjustment();
            }
            newRating.setPlayerRatingAdjustmentId(null);
            newRating.setInitialRating(v.getFinalRating());
            newRating.setFirstPassRating(v.getFinalRating());
            newRating.setFinalRating(v.getFinalRating());
            result.put(v.getPlayerId(), newRating);
        });

        // now apply the new ratings
        matchResultList.forEach(m -> {
            final PlayerRatingAdjustment winnerRating = result.get(m.getWinnerId());
            winnerRating.setFinalRating(winnerRating.getFinalRating() + m.getWinnerRatingDelta());
            final PlayerRatingAdjustment loserRating = result.get(m.getLoserId());
            loserRating.setFinalRating(loserRating.getFinalRating() - m.getWinnerRatingDelta());
        });

        return result;
    }

    public Map<String, PlayerRatingAdjustment> getPlayerRatingAdjustmentMap(
            final TournamentResultLineItem[] tournamentResultList) {
        final Map<String, PlayerRatingAdjustment> map = new HashMap<>();

        final Set<String> playerSet = getPlayerSet(tournamentResultList);

        playerSet.forEach(p -> {
            final Optional<PlayerRatingAdjustment> rating = playerRepository.findByUserName(p)
                    .map(Player::getPlayerId)
                    .flatMap(this::getRating);

            if (rating.isPresent()) {
                map.put(p, rating.get());
            }
        });

        return map;
    }

    /**
     * returns a set of players who played in the tournament
     * @param tournamentResultList
     * @return
     */
    public Set<String> getPlayerSet(final TournamentResultLineItem[] tournamentResultList) {
        final Set<String> playerSet = new HashSet<>();
        for(TournamentResultLineItem result : tournamentResultList) {
            playerSet.add(result.getWinner());
            playerSet.add(result.getLoser());
        }
        return playerSet;
    }

    @Transactional
    public TournamentResultResponse submitTournamentResultWithPlayerFinder(
            final TournamentResultRequest tournamentResultRequest,
            final Function<String, Optional<Player>> playerFinder)
            throws DuplicateTournamentException {
        final String tournamentName = tournamentResultRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been submitted",
                    tournamentName));
        }

        final Tournament tournament = new Tournament();
        tournament.setName(tournamentName);
        tournament.setTournamentDate(tournamentResultRequest.getTournamentDate());
        final Tournament savedTournament = tournamentRepository.save(tournament);

        final TournamentResultResponse result = new TournamentResultResponse();

        result.setTournamentName(tournamentResultRequest.getTournamentName());
        result.setTournamentDate(tournamentResultRequest.getTournamentDate());
        result.setTournamentId(savedTournament.getTournamentId());

        final TournamentResultLineItem[] tournamentResultList = tournamentResultRequest.getTournamentResultList();

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                getPlayerRatingAdjustmentMap(tournamentResultList);

        final List<TournamentResultLineItemResponse> tournamentResultLineItemResponseList =
                Arrays.stream(tournamentResultList).map(lineItem -> {
                    final TournamentResultLineItemResponse tournamentResultLineItemResponse =
                            new TournamentResultLineItemResponse();
                    if (!getPlayer(lineItem.getWinner()).isPresent()) {
                        tournamentResultLineItemResponse.setRejectReason(TournamentResultLineItemResponse.REJECT_REASON_INVALID_WINNER);
                        tournamentResultLineItemResponse.setProcessed(false);
                        return tournamentResultLineItemResponse;
                    }
                    if (!getPlayer(lineItem.getLoser()).isPresent()) {
                        tournamentResultLineItemResponse.setRejectReason(TournamentResultLineItemResponse.REJECT_REASON_INVALID_LOSER);
                        tournamentResultLineItemResponse.setProcessed(false);
                        return tournamentResultLineItemResponse;
                    }
                    final MatchResult matchResult = generateMatchResult(playerRatingAdjustmentMap, lineItem);
                    tournamentResultLineItemResponse.setOriginalTournamentResultLineItem(lineItem);
                    tournamentResultLineItemResponse.setMatchResult(matchResult);
                    tournamentResultLineItemResponse.setProcessed(true);
                    return tournamentResultLineItemResponse;
                }).collect(Collectors.toList());
        result.setTournamentResultLineItemResponseList(tournamentResultLineItemResponseList);

        final List<MatchResult> matchResultList = tournamentResultLineItemResponseList.stream()
                .filter(TournamentResultLineItemResponse::isProcessed)
                .map(TournamentResultLineItemResponse::getMatchResult).collect(Collectors.toList());

        final Map<Integer, PlayerRatingAdjustment> newPlayerRatingAdjustmentMap =
                applyMatchResultList(playerRatingAdjustmentMap, matchResultList);

        newPlayerRatingAdjustmentMap.values().forEach(adjustment -> {
            adjustment.setTournamentId(savedTournament.getTournamentId());
            adjustment.setAdjustmentDate(savedTournament.getTournamentDate());
            addPlayerRatingAdjustment(adjustment);
        });
        result.setPlayerRatingList(new ArrayList<>(newPlayerRatingAdjustmentMap.values()));

        return result;
    }

    public TournamentResultResponse submitTournamentResult(final TournamentResultRequest tournamentResultRequest,
            boolean autoAddPlayer)
            throws DuplicateTournamentException {
        if (autoAddPlayer) {
            return submitTournamentResultWithPlayerFinder(tournamentResultRequest, this::getOrCreatePlayer);
        } else {
            return submitTournamentResultWithPlayerFinder(tournamentResultRequest, this::getPlayer);
        }
    }
}