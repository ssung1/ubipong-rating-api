package com.eatsleeppong.ubipong.rating.manager;

import com.eatsleeppong.ubipong.rating.controller.DuplicateTournamentException;
import com.eatsleeppong.ubipong.rating.controller.RatingInputFormatException;
import com.eatsleeppong.ubipong.rating.entity.MatchResult;
import com.eatsleeppong.ubipong.rating.entity.Player;
import com.eatsleeppong.ubipong.rating.entity.Tournament;
import com.eatsleeppong.ubipong.rating.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.rating.model.*;
import com.eatsleeppong.ubipong.rating.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.rating.repository.PlayerRepository;
import com.eatsleeppong.ubipong.rating.repository.TournamentRepository;
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
        final List<RatingAdjustmentRequestLineItem> playerRatingList = ratingAdjustmentRequest.getRatingAdjustmentList();
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                new ArrayList<>(playerRatingList.size());

        final String tournamentName = ratingAdjustmentRequest.getTournamentName();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been recorded",
                tournamentName));
        }

        result.setTournamentDate(ratingAdjustmentRequest.getTournamentDate());
        result.setTournamentName(ratingAdjustmentRequest.getTournamentName());

        for (RatingAdjustmentRequestLineItem playerRating : playerRatingList) {
            final RatingAdjustmentResponseLineItem playerRatingResult = new RatingAdjustmentResponseLineItem();
            playerRatingResult.setOriginalRequest(playerRating);

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = getPlayer(playerUserName);
            if (!player.isPresent()) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER);
                ratingAdjustmentResponseLineItemList.add(playerRatingResult);
                continue;
            }

            try {
                final Integer rating = Integer.parseInt(playerRating.getRating());
                if (rating < 0) {
                    playerRatingResult.setProcessed(false);
                    playerRatingResult.setRejectReason(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING);
                    ratingAdjustmentResponseLineItemList.add(playerRatingResult);
                }
            } catch (Exception ex) {
                playerRatingResult.setProcessed(false);
                playerRatingResult.setRejectReason(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING);
                ratingAdjustmentResponseLineItemList.add(playerRatingResult);
            }
        }

        result.setRatingAdjustmentResponseList(ratingAdjustmentResponseLineItemList);
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
        final List<RatingAdjustmentRequestLineItem> ratingAdjustmentRequestLineItemList = new ArrayList<>();
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
                final RatingAdjustmentRequestLineItem playerRating = new RatingAdjustmentRequestLineItem();
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

                    ratingAdjustmentRequestLineItemList.add(playerRating);
                } catch (Exception ex) {
                    // can't do much
                }
            }
        }

        result.setRatingAdjustmentList(ratingAdjustmentRequestLineItemList);
        return result;
    }

    @Transactional
    public RatingAdjustmentResponse adjustRatingByCsv(final String csv, final boolean autoAddPlayer)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {
        final RatingAdjustmentRequest ratingAdjustmentRequest = convertCsvToRatingAdjustmentRequest(csv);
        return adjustRating(ratingAdjustmentRequest, autoAddPlayer);
    }

    /**
     * called exclusively by adjustRatingWithPlayerFinder to create a RatingAdjustmentResponseLineItem
     * for every RatingAdjustmentRequestLineItem
     * @param ratingAdjustmentRequestLineItemList
     * @return
     */
    private List<RatingAdjustmentResponseLineItem> processRatingAdjustmentResponseLineItemList(
            final List<RatingAdjustmentRequestLineItem> ratingAdjustmentRequestLineItemList,
            final Function<String, Optional<Player>> playerFinder) {
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                new ArrayList<>(ratingAdjustmentRequestLineItemList.size());
        for (RatingAdjustmentRequestLineItem playerRating : ratingAdjustmentRequestLineItemList) {
            final RatingAdjustmentResponseLineItem ratingAdjustmentResponseLineItem = new RatingAdjustmentResponseLineItem();
            ratingAdjustmentResponseLineItemList.add(ratingAdjustmentResponseLineItem);
            ratingAdjustmentResponseLineItem.setOriginalRequest(playerRating);

            final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

            final String playerUserName = playerRating.getPlayerUserName();
            final Optional<Player> player = playerFinder.apply(playerUserName);
            if (player.isPresent()) {
                playerRatingAdjustment.setPlayerId(player.get().getPlayerId());
            } else {
                // we have decided to not throw exception in this case since we are prepared to handle cases where
                // player does not exist.  this is following the principle that exceptions are only meant for
                // unexpected errors.
                ratingAdjustmentResponseLineItem.setProcessed(false);
                ratingAdjustmentResponseLineItem.setRejectReason(
                        RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER);
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
            } catch (Exception ex) {
                ratingAdjustmentResponseLineItem.setProcessed(false);
                ratingAdjustmentResponseLineItem.setRejectReason(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING);
                continue;
            }

            ratingAdjustmentResponseLineItem.setAdjustmentResult(playerRatingAdjustment);
            ratingAdjustmentResponseLineItem.setProcessed(true);
        }

        return ratingAdjustmentResponseLineItemList;
    }

    @Transactional
    private RatingAdjustmentResponse adjustRatingWithPlayerFinder(
            final RatingAdjustmentRequest ratingAdjustmentRequest,
            final Function<String, Optional<Player>> playerFinder)
            throws DuplicateTournamentException {
        final String tournamentName = ratingAdjustmentRequest.getTournamentName();
        final Date tournamentDate = ratingAdjustmentRequest.getTournamentDate();

        final RatingAdjustmentResponse result = new RatingAdjustmentResponse();
        result.setTournamentDate(tournamentDate);
        result.setTournamentName(tournamentName);

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament \"{0}\" has already been recorded",
                    tournamentName));
        }

        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                processRatingAdjustmentResponseLineItemList(ratingAdjustmentRequest.getRatingAdjustmentList(),
                        playerFinder);

        final boolean isAllProcessed = ratingAdjustmentResponseLineItemList.stream()
                .allMatch(RatingAdjustmentResponseLineItem::isProcessed);
        // we only do the database operation if all the records pass sanity check
        if (isAllProcessed) {
            final Tournament tournament = new Tournament();
            tournament.setName(tournamentName);
            tournament.setTournamentDate(tournamentDate);
            final Tournament savedTournament = tournamentRepository.save(tournament);

            result.setTournamentId(savedTournament.getTournamentId());

            ratingAdjustmentResponseLineItemList.forEach(adj -> {
                adj.getAdjustmentResult().setTournamentId(savedTournament.getTournamentId());
                adj.getAdjustmentResult().setAdjustmentDate(tournamentDate);

                // .getAdjustmentResult is where we keep the PlayerRatingAdjustment entities
                final PlayerRatingAdjustment savedAdj = playerRatingAdjustmentRepository.save(
                        adj.getAdjustmentResult());
                // savedAdj has the ID
                adj.setAdjustmentResult(savedAdj);
            });
            result.setRatingAdjustmentResponseList(ratingAdjustmentResponseLineItemList);
            result.setProcessed(true);
        } else {
            final List<RatingAdjustmentResponseLineItem> errorList = ratingAdjustmentResponseLineItemList
                    .stream().filter(r -> !r.isProcessed())
                    .collect(Collectors.toList());
            result.setRatingAdjustmentResponseList(errorList);
        }

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
     * @param tournamentResultRequestLineItem
     * @return
     */
    public MatchResult generateMatchResult(
            final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap,
            final TournamentResultRequestLineItem tournamentResultRequestLineItem) {
        final String winnerUserName = tournamentResultRequestLineItem.getWinner();
        final String loserUserName = tournamentResultRequestLineItem.getLoser();

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

    /**
     * @param playerSet set of players in the tournament.  we need to find their ratings.
     * @return a map of players and their ratings.  if the player has no rating, the map will not include the player
     */
    public Map<String, PlayerRatingAdjustment> getPlayerRatingAdjustmentMap(
            final Set<String> playerSet) {
        final Map<String, PlayerRatingAdjustment> map = new HashMap<>();

        playerSet.forEach(p -> {
            final Optional<PlayerRatingAdjustment> rating = playerRepository.findByUserName(p)
                    .map(Player::getPlayerId)
                    .flatMap(this::getRating);

            rating.map(r -> map.put(p, r));
        });

        return map;
    }

    /**
     * returns a set of players who played in the tournament
     * @param tournamentResultList
     * @return
     */
    public Set<String> getPlayerSet(final TournamentResultRequestLineItem[] tournamentResultList) {
        final Set<String> playerSet = new HashSet<>();
        for(TournamentResultRequestLineItem result : tournamentResultList) {
            playerSet.add(result.getWinner());
            playerSet.add(result.getLoser());
        }
        return playerSet;
    }

    /**
     * Called exclusively by submitTournamentResultWithPlayerFinder to create a TournamentResultResponseLineItem
     * for each TournamentResultRequestLineItem
     *
     * The most important part of the response is the MatchResult, which contains the amount of the rating transfer
     * between the players
     *
     * @param tournamentResultRequestLineItemList
     * @param playerRatingAdjustmentMap
     * @param playerFinder
     * @return
     */
    private List<TournamentResultResponseLineItem> processTournamentResultRequestLineItemList(
            final TournamentResultRequestLineItem[] tournamentResultRequestLineItemList,
            final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap,
            final Function<String, Optional<Player>> playerFinder) {
        return Arrays.stream(tournamentResultRequestLineItemList).map(lineItem -> {
            final TournamentResultResponseLineItem tournamentResultResponseLineItem =
                    new TournamentResultResponseLineItem();
            tournamentResultResponseLineItem.setOriginalTournamentResultLineItem(lineItem);
            if (!playerFinder.apply(lineItem.getWinner()).isPresent()) {
                tournamentResultResponseLineItem.setRejectReason(
                        TournamentResultResponseLineItem.REJECT_REASON_INVALID_WINNER);
                tournamentResultResponseLineItem.setProcessed(false);
                return tournamentResultResponseLineItem;
            }
            if (!playerFinder.apply(lineItem.getLoser()).isPresent()) {
                tournamentResultResponseLineItem.setRejectReason(
                        TournamentResultResponseLineItem.REJECT_REASON_INVALID_LOSER);
                tournamentResultResponseLineItem.setProcessed(false);
                return tournamentResultResponseLineItem;
            }
            final MatchResult matchResult = generateMatchResult(playerRatingAdjustmentMap, lineItem);
            tournamentResultResponseLineItem.setMatchResult(matchResult);
            tournamentResultResponseLineItem.setProcessed(true);
            return tournamentResultResponseLineItem;
        }).collect(Collectors.toList());
    }

    @Transactional
    private TournamentResultResponse submitTournamentResultWithPlayerFinder(
            final TournamentResultRequest tournamentResultRequest,
            final Function<String, Optional<Player>> playerFinder)
            throws DuplicateTournamentException {
        final String tournamentName = tournamentResultRequest.getTournamentName();
        final Date tournamentDate = tournamentResultRequest.getTournamentDate();

        if (getTournament(tournamentName).isPresent()) {
            throw new DuplicateTournamentException(MessageFormat.format("Tournament '{0}' has already been submitted",
                    tournamentName));
        }

        final TournamentResultResponse result = new TournamentResultResponse();

        result.setTournamentName(tournamentName);
        result.setTournamentDate(tournamentDate);

        final TournamentResultRequestLineItem[] tournamentResultList =
                tournamentResultRequest.getTournamentResultList();

        // create a map of player ratings; these are needed to calculate rating adjustments
        final Set<String> playerSet = getPlayerSet(tournamentResultList);
        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap = getPlayerRatingAdjustmentMap(playerSet);

        // TournamentResultResponseLineItem contains MatchResult, which includes the amount of the rating transfer
        final List<TournamentResultResponseLineItem> tournamentResultResponseLineItemList =
                processTournamentResultRequestLineItemList(tournamentResultList, playerRatingAdjustmentMap,
                        playerFinder);

        final boolean isAllProcessed = tournamentResultResponseLineItemList.stream()
                .allMatch(TournamentResultResponseLineItem::isProcessed);

        // only apply the tournament results all of the results have been validated
        if (isAllProcessed) {
            final List<MatchResult> matchResultList = tournamentResultResponseLineItemList.stream()
                    .filter(TournamentResultResponseLineItem::isProcessed)
                    .map(TournamentResultResponseLineItem::getMatchResult).collect(Collectors.toList());

            // MatchResult only has the deltas
            // We need to call applyMatchResultList to get the final ratings
            final Map<Integer, PlayerRatingAdjustment> newPlayerRatingAdjustmentMap =
                    applyMatchResultList(playerRatingAdjustmentMap, matchResultList);

            final Tournament tournament = new Tournament();
            tournament.setName(tournamentName);
            tournament.setTournamentDate(tournamentDate);
            final Tournament savedTournament = tournamentRepository.save(tournament);

            result.setTournamentId(savedTournament.getTournamentId());

            newPlayerRatingAdjustmentMap.values().forEach(adjustment -> {
                adjustment.setTournamentId(savedTournament.getTournamentId());
                adjustment.setAdjustmentDate(savedTournament.getTournamentDate());
                addPlayerRatingAdjustment(adjustment);
            });

            result.setTournamentResultResponseList(tournamentResultResponseLineItemList);
            result.setRatingAdjustmentList(new ArrayList<>(newPlayerRatingAdjustmentMap.values()));
        } else {
            final List<TournamentResultResponseLineItem> errorList = tournamentResultResponseLineItemList.stream()
                    .filter(r -> !r.isProcessed())
                    .collect(Collectors.toList());
            result.setTournamentResultResponseList(errorList);
        }

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