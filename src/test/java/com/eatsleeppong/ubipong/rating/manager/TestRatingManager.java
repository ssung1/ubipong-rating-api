package com.eatsleeppong.ubipong.rating.manager;

import com.eatsleeppong.ubipong.rating.controller.DuplicateTournamentException;
import com.eatsleeppong.ubipong.rating.entity.MatchResult;
import com.eatsleeppong.ubipong.rating.entity.Player;
import com.eatsleeppong.ubipong.rating.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.rating.entity.Tournament;
import com.eatsleeppong.ubipong.rating.model.*;
import name.subroutine.etable.CsvTable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TestRatingManager {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private RatingManager ratingManager;

    private Integer spongeBobId = 1;
    private Integer patrickId = 2;
    private Integer squidwardId = 3;

    private String spongeBobUserName = "spongebob";
    private String patrickUserName = "patrick";
    private String squidwardUserName = "squidward";

    private Integer usOpenId = 100;
    private Integer atlantaOpenId = 101;

    private Player spongeBob;
    private Player patrick;
    private Player squidward;

    private final String tournamentName1 = "test-tournament-1";
    private final String tournamentName2 = "test-tournament-2";

    private final String tournamentDate1 = "2018-12-28T00:00:00-0500";
    private final String tournamentDate2 = "2019-01-12T00:00:00-0500";

    @Before
    public void setup() throws ParseException {
        spongeBob = new Player();
        spongeBob.setUserName(spongeBobUserName);

        patrick = new Player();
        patrick.setUserName(patrickUserName);

        squidward = new Player();
        squidward.setUserName(squidwardUserName);
    }

    /**
     * add SpongeBob and Patrick to the database with an initial rating
     * @param spongeBobFinalRating
     * @param patrickFinalRating
     * @throws ParseException
     */
    private void initializeSpongeBobAndPatrick(final Integer spongeBobFinalRating, final Integer patrickFinalRating)
            throws ParseException {
        // set up current rating
        final Integer spongeBobId = ratingManager.addPlayer(spongeBob).getPlayerId();
        final Integer patrickId = ratingManager.addPlayer(patrick).getPlayerId();

        final PlayerRatingAdjustment spongeBobRating = new PlayerRatingAdjustment();
        spongeBobRating.setPlayerId(spongeBobId);
        spongeBobRating.setAdjustmentDate(df.parse(tournamentDate1));
        spongeBobRating.setInitialRating(spongeBobFinalRating);
        spongeBobRating.setFirstPassRating(spongeBobFinalRating);
        spongeBobRating.setFinalRating(spongeBobFinalRating);

        final PlayerRatingAdjustment patrickRating = new PlayerRatingAdjustment();
        patrickRating.setPlayerId(patrickId);
        patrickRating.setAdjustmentDate(df.parse(tournamentDate1));
        patrickRating.setInitialRating(patrickFinalRating);
        patrickRating.setFirstPassRating(patrickFinalRating);
        patrickRating.setFinalRating(patrickFinalRating);

        assertThat(ratingManager.addPlayerRatingAdjustment(spongeBobRating), notNullValue());
        assertThat(ratingManager.addPlayerRatingAdjustment(patrickRating), notNullValue());

        // make sure we set up rating correctly
        final PlayerRatingAdjustment spongeBobSavedRating = ratingManager.getRating(spongeBobId).orElseThrow(
            () -> new AssertionError("Cannot get final rating")
        );

        assertThat(spongeBobSavedRating.getFinalRating(), is(spongeBobFinalRating));

        final PlayerRatingAdjustment patrickSavedRating = ratingManager.getRating(patrickId).orElseThrow(
            () -> new AssertionError("Cannot get final rating")
        );

        assertThat(patrickSavedRating.getFinalRating(), is(patrickFinalRating));
    }

    /**
     * Create PlayerRatingAdjustment records for SpongeBog and Patrick.  These are used when processing tournament
     * results.
     * @param spongeBobFinalRating
     * @param patrickFinalRating
     * @return
     */
    private Map<String, PlayerRatingAdjustment> createPlayerRatingAdjustmentForSpongeBobAndPatrick(
            final Integer spongeBobFinalRating, final Integer patrickFinalRating) {
        final PlayerRatingAdjustment spongeBobRating = new PlayerRatingAdjustment();
        spongeBobRating.setPlayerId(spongeBobId);
        spongeBobRating.setInitialRating(0);
        spongeBobRating.setFirstPassRating(0);
        spongeBobRating.setFinalRating(spongeBobFinalRating);

        final PlayerRatingAdjustment patrickRating = new PlayerRatingAdjustment();
        patrickRating.setPlayerId(patrickId);
        patrickRating.setInitialRating(0);
        patrickRating.setFirstPassRating(0);
        patrickRating.setFinalRating(patrickFinalRating);

        return new HashMap<String, PlayerRatingAdjustment>() {{
                    put(spongeBobUserName, spongeBobRating);
                    put(patrickUserName, patrickRating);
        }};
    }

    /**
     * Create TournamentResultRequest for SpongeBog and Patrick
     * @return
     */
    private TournamentResultRequest createTournamentResultRequestForSpongeBobAndPatrick() throws ParseException {
        final TournamentResultRequestLineItem tournamentResultRequestLineItem = new TournamentResultRequestLineItem();
        tournamentResultRequestLineItem.setWinner(spongeBobUserName);
        tournamentResultRequestLineItem.setLoser(patrickUserName);

        final TournamentResultRequest tournamentResultRequest = new TournamentResultRequest();
        tournamentResultRequest.setTournamentName(tournamentName2);
        tournamentResultRequest.setTournamentDate(df.parse(tournamentDate2));
        tournamentResultRequest.setTournamentResultList(new TournamentResultRequestLineItem[] {
                tournamentResultRequestLineItem,
        });

        return tournamentResultRequest;
    }

    @Test
    public void canary() {

    }

    @Test
    public void testCsvParserQuotedFirstCell() {
        final String[] result = CsvTable.toArray("\"a\"");
        assertThat(result[0], is("a"));
    }

    @Test
    public void testCsvParserQuotedSecondCell() {
        final String[] result = CsvTable.toArray("x,\"a\"");
        assertThat(result[1], is("a"));
    }

    @Test
    public void getPlayerRating() throws Exception {
        final Integer expectedFinalRating = 1200;

        initializeSpongeBobAndPatrick(expectedFinalRating, expectedFinalRating);

        final PlayerRatingAdjustment finalRating = ratingManager.getPlayer(spongeBobUserName)
                .flatMap(p -> ratingManager.getRating(p.getPlayerId())).orElseThrow(
            () -> new AssertionError("Cannot get final rating")
        );
        assertThat(finalRating.getFinalRating(), is(expectedFinalRating));
    }

    @Test
    public void addPlayer() {
        final Player bob = ratingManager.addPlayer(spongeBob);

        assertThat(bob.getPlayerId(), notNullValue());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void userNameMustBeUnique() {
        final String sameUserName = "same_user_name";
        final Player a = new Player();
        a.setUserName(sameUserName);

        final Player b = new Player();
        b.setUserName(sameUserName);

        ratingManager.addPlayer(a);
        ratingManager.addPlayer(b);

        ratingManager.getPlayer(sameUserName);
    }

    @Test
    public void getPlayerByUserName() {
        ratingManager.addPlayer(spongeBob);

        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);

        assertTrue(spongebob.isPresent());
        assertThat(spongebob.get().getUserName(), is(spongeBobUserName));
        assertThat(spongebob.map(Player::getUserName).orElse(null), is(spongeBobUserName));
    }

    @Test
    public void getPlayerById() {
        final Player spongebob1 = ratingManager.addPlayer(spongeBob);

        final Optional<Player> spongebob2 = ratingManager.getPlayerById(spongebob1.getPlayerId());

        assertThat(spongebob2.map(Player::getUserName).orElse(null), is(spongeBobUserName));
    }

    @Test
    public void getPlayerByInvalidUserName() {
        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);

        assertFalse(spongebob.isPresent());
    }

    @Test
    public void adjustPlayerRatingByCsv() throws Exception {
        final Integer spongeBobId = ratingManager.addPlayer(spongeBob).getPlayerId();
        final Integer patrickId = ratingManager.addPlayer(patrick).getPlayerId();

        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, 2019-01-01T00:00:00-0500\n" +
                "player, rating\n" +
                "spongebob,   1000\n" +
                "\"patrick\", 1100\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, false);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                ratingAdjustmentResponse.getRatingAdjustmentResponseList();

        assertTrue(ratingAdjustmentResponse.isProcessed());
        assertTrue(ratingAdjustmentResponseLineItemList.get(0).isProcessed());
        assertThat(ratingAdjustmentResponseLineItemList.get(0).getOriginalRequest().getPlayerUserName(), is("spongebob"));
        assertThat(ratingAdjustmentResponseLineItemList.get(0).getOriginalRequest().getRating(), is("1000"));
        assertTrue(ratingAdjustmentResponseLineItemList.get(1).isProcessed());
        assertThat(ratingAdjustmentResponseLineItemList.get(1).getOriginalRequest().getPlayerUserName(), is("patrick"));
        assertThat(ratingAdjustmentResponseLineItemList.get(1).getOriginalRequest().getRating(), is("1100"));

        assertTrue(ratingManager.getTournament(tournamentName1).isPresent());
        final Integer spongeBobRating = ratingManager.getRating(spongeBobId)
                .map(PlayerRatingAdjustment::getFinalRating).orElse(0);
        final Integer patrickRating = ratingManager.getRating(patrickId)
            .map(PlayerRatingAdjustment::getFinalRating).orElse(0);

        assertThat(spongeBobRating, is(1000));
        assertThat(patrickRating, is(1100));
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidRating() throws Exception {
        ratingManager.addPlayer(spongeBob).getPlayerId();

        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                spongeBobUserName + ",      asdf\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, false);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
            ratingAdjustmentResponse.getRatingAdjustmentResponseList();
        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertFalse(ratingAdjustmentResponse.isProcessed());
        assertFalse(playerRatingResult.isProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING));
        assertThat(playerRatingResult.getOriginalRequest().getPlayerUserName(), is(spongeBobUserName));

        assertFalse(ratingManager.getTournament(tournamentName1).isPresent());
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerNoAutoAdd() throws Exception {
        final String inputString =
                "tournamentName, test-tournament-1\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, false);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
            ratingAdjustmentResponse.getRatingAdjustmentResponseList();
        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertFalse(ratingAdjustmentResponse.isProcessed());
        assertFalse(playerRatingResult.isProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER));

        assertFalse(ratingManager.getTournament(tournamentName1).isPresent());
    }

    /**
     * All-or-none adjustment: if there is an error, no records are inserted
     * @throws Exception
     */
    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerNoAutoAddGoodRecordNotAdded() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,    1000\n" +
                "patrick,      1000\n";

        final Integer patrickId = ratingManager.addPlayer(patrick).getPlayerId();

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, false);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                ratingAdjustmentResponse.getRatingAdjustmentResponseList();

        assertFalse(ratingAdjustmentResponse.isProcessed());
        // if there is an error, only errors are reported
        assertThat(ratingAdjustmentResponseLineItemList, hasSize(1));

        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertFalse(playerRatingResult.getProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER));

        final Optional<Tournament> tournament = ratingManager.getTournament(tournamentName1);
        assertFalse("Tournament should not have been created because spongebob rating failed to add",
                tournament.isPresent());
        final Optional<PlayerRatingAdjustment> patrickRating = ratingManager.getRating(patrickId);
        assertFalse("Patrick rating should not be added because spongebob rating failed to add",
                patrickRating.isPresent());
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidRatingNoRecordsAdded() throws Exception {
        final String inputString =
                "tournamentName, test-tournament-1\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "\"inva\"\"lid\",       Rating\n" +
                "patrick,      1000\n";

        final Integer patrickId = ratingManager.addPlayer(patrick).getPlayerId();

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, true);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                ratingAdjustmentResponse.getRatingAdjustmentResponseList();
        // if there is an error, only errors are reported
        assertThat(ratingAdjustmentResponseLineItemList, hasSize(1));

        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertFalse(playerRatingResult.getProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING));

        final Optional<Tournament> tournament = ratingManager.getTournament(tournamentName1);
        assertFalse("Tournament should not have been created because a rating was invalid",
                tournament.isPresent());
        final Optional<PlayerRatingAdjustment> patrickRating = ratingManager.getRating(patrickId);
        assertFalse("Patrick rating should not be added because another record has invalid rating",
                patrickRating.isPresent());
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerWithAutoAdd() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, true);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
            ratingAdjustmentResponse.getRatingAdjustmentResponseList();
        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertTrue(ratingAdjustmentResponse.isProcessed());
        assertTrue(playerRatingResult.isProcessed());
        assertTrue(ratingManager.getTournament(tournamentName1).isPresent());

        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);

        final Integer spongeBobRating = spongebob
                .flatMap(p -> ratingManager.getRating(p.getPlayerId()))
                .map(PlayerRatingAdjustment::getFinalRating)
                .orElse(0);

        assertThat(spongeBobRating, is(1000));
    }

    @Test
    public void adjustPlayerRatingByCsvSetTournamentInfo() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, true);

        assertThat(ratingAdjustmentResponse.getTournamentName(), is(tournamentName1));
        assertThat(ratingAdjustmentResponse.getTournamentDate(), is(df.parse(tournamentDate1)));
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
            ratingAdjustmentResponse.getRatingAdjustmentResponseList();
        final RatingAdjustmentResponseLineItem playerRatingResult = ratingAdjustmentResponseLineItemList.get(0);

        assertTrue(playerRatingResult.getProcessed());

        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);
        final Date spongeBobAdjustmentDate = spongebob
            .flatMap(p -> ratingManager.getRating(p.getPlayerId()))
            .map(PlayerRatingAdjustment::getAdjustmentDate)
            .orElse(null);

        assertThat(spongeBobAdjustmentDate, is(df.parse(tournamentDate1)));
    }

    @Test
    public void adjustPlayerRatingByCsvAutoAddTournament() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        ratingManager.adjustRatingByCsv(inputString, true);

        final Optional<Tournament> tournament = ratingManager.getTournament(tournamentName1);

        assertThat(tournament.isPresent(), is(true));
        assertThat(tournament.get().getName(), is(tournamentName1));
        assertThat(tournament.get().getTournamentId(), is(greaterThan(0)));

        final Optional<Player> spongeBob = ratingManager.getPlayer(spongeBobUserName);

        assertThat(spongeBob.isPresent(), is(true));

        final Optional<PlayerRatingAdjustment> rating = ratingManager.getRating(
                spongeBob.map(Player::getPlayerId).orElse(0));

        assertThat(rating.map(PlayerRatingAdjustment::getTournamentId).orElse(0),
                is(tournament.get().getTournamentId()));
    }

    @Test
    public void multipleErrorRecords() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n" +
                "patrick,        \n";

        ratingManager.addPlayer(patrick);

        final RatingAdjustmentResponse ratingAdjustmentResponse = ratingManager.adjustRatingByCsv(inputString, false);
        final List<RatingAdjustmentResponseLineItem> ratingAdjustmentResponseLineItemList =
                ratingAdjustmentResponse.getRatingAdjustmentResponseList();

        assertFalse(ratingAdjustmentResponse.isProcessed());
        assertThat(ratingAdjustmentResponseLineItemList.get(0).getRejectReason(),
                is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER));
        assertThat(ratingAdjustmentResponseLineItemList.get(1).getRejectReason(),
            is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_RATING));
    }

    @Test(expected = DuplicateTournamentException.class)
    public void failIfDuplicateTournament() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        ratingManager.adjustRatingByCsv(inputString, true);
        ratingManager.adjustRatingByCsv(inputString, true);
    }

    @Test
    public void adjustPlayerRatingByCsvCalculateInitialRating() throws Exception {
        final Integer spongeBobId = ratingManager.addPlayer(spongeBob).getPlayerId();

        final String tournament1 =
                "tournamentName, test-tournament-1\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";
        final String tournament2 =
                "tournamentName, test-tournament-2\n" +
                "date, " + tournamentDate2 + "\n" +
                "player, rating\n" +
                "spongebob,      1100\n";

        ratingManager.adjustRatingByCsv(tournament1, false);
        // when adding a second tournament, the initial rating is taken from the final rating of first tournament
        ratingManager.adjustRatingByCsv(tournament2, false);

        final List<PlayerRatingAdjustment> ratingHistory = ratingManager.getRatingHistory(spongeBobId, 2);

        assertThat(ratingHistory.get(0).getFinalRating(), is(1100));
        assertThat(ratingHistory.get(0).getFirstPassRating(), is(1000));
        assertThat(ratingHistory.get(0).getInitialRating(), is(1000));

        assertThat(ratingHistory.get(1).getFinalRating(), is(1000));
        assertThat(ratingHistory.get(1).getFirstPassRating(), is(0));
        assertThat(ratingHistory.get(1).getInitialRating(), is(0));
    }

    @Test
    public void adjustPlayerRatingByJson() throws Exception {
        final RatingAdjustmentRequest ratingAdjustmentRequest = new RatingAdjustmentRequest();
        ratingAdjustmentRequest.setTournamentName(tournamentName1);
        ratingAdjustmentRequest.setTournamentDate(df.parse(tournamentDate1));

        final RatingAdjustmentRequestLineItem spongeBobRatingLineItem = new RatingAdjustmentRequestLineItem();
        spongeBobRatingLineItem.setPlayerUserName(spongeBobUserName);
        spongeBobRatingLineItem.setRating("1234");

        ratingAdjustmentRequest.setRatingAdjustmentList(Collections.singletonList(spongeBobRatingLineItem));

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRating(ratingAdjustmentRequest, true);

        assertThat(ratingAdjustmentResponse.getTournamentName(), is(tournamentName1));
        assertThat(ratingAdjustmentResponse.getTournamentDate(), is(df.parse(tournamentDate1)));
        assertThat(ratingAdjustmentResponse.getTournamentId(), notNullValue());

        final PlayerRatingAdjustment playerRatingAdjustment =
                ratingAdjustmentResponse.getRatingAdjustmentResponseList().get(0).getAdjustmentResult();
        assertThat(playerRatingAdjustment.getFinalRating(), is(1234));
    }

    @Test
    public void generateMatchResult() {
        final Integer spongeBobFinalRating = 1000;
        final Integer patrickFinalRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                createPlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobFinalRating, patrickFinalRating);

        final TournamentResultRequestLineItem tournamentResultRequestLineItem = new TournamentResultRequestLineItem();
        tournamentResultRequestLineItem.setWinner(spongeBobUserName);
        tournamentResultRequestLineItem.setLoser(patrickUserName);

        final MatchResult matchResult = ratingManager.generateMatchResult(playerRatingAdjustmentMap,
                tournamentResultRequestLineItem);
        assertThat(matchResult.getWinnerId(), is(spongeBobId));
        assertThat(matchResult.getLoserId(), is(patrickId));
        assertThat(matchResult.getWinnerRatingDelta(), is(20));
    }

    @Test
    public void applyEmptyMatchResultList() {
        final Integer spongeBobFinalRating = 1000;
        final Integer patrickFinalRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                createPlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobFinalRating, patrickFinalRating);

        final Map<Integer, PlayerRatingAdjustment> newRatingMap =
                ratingManager.applyMatchResultList(playerRatingAdjustmentMap, Collections.emptyList());

        final PlayerRatingAdjustment spongeBobNewRating = newRatingMap.get(spongeBobId);
        final PlayerRatingAdjustment patrickNewRating = newRatingMap.get(patrickId);

        assertThat(spongeBobNewRating.getFinalRating(), is(spongeBobFinalRating));
        assertThat(patrickNewRating.getFinalRating(), is(patrickFinalRating));
    }

    @Test
    public void applyMatchResultListSize1() {
        final Integer spongeBobFinalRating = 1000;
        final Integer patrickFinalRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
            createPlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobFinalRating, patrickFinalRating);

        final MatchResult matchResult = new MatchResult();
        matchResult.setWinnerId(spongeBobId);
        matchResult.setLoserId(patrickId);
        matchResult.setWinnerRatingDelta(20);

        final Map<Integer, PlayerRatingAdjustment> newRatingMap =
            ratingManager.applyMatchResultList(playerRatingAdjustmentMap, Collections.singletonList(matchResult));

        final PlayerRatingAdjustment spongeBobNewRating = newRatingMap.get(spongeBobId);
        final PlayerRatingAdjustment patrickNewRating = newRatingMap.get(patrickId);

        assertThat(spongeBobNewRating.getFinalRating(), is(1020));
        assertThat(patrickNewRating.getFinalRating(), is(1080));
    }

    @Test
    public void applyMatchResultListSize2() {
        final Integer spongeBobFinalRating = 1000;
        final Integer patrickFinalRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
            createPlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobFinalRating, patrickFinalRating);

        final MatchResult matchResult1 = new MatchResult();
        matchResult1.setWinnerId(spongeBobId);
        matchResult1.setLoserId(patrickId);
        matchResult1.setWinnerRatingDelta(20);

        final MatchResult matchResult2 = new MatchResult();
        matchResult2.setWinnerId(patrickId);
        matchResult2.setLoserId(spongeBobId);
        matchResult2.setWinnerRatingDelta(4);

        final Map<Integer, PlayerRatingAdjustment> newRatingMap =
            ratingManager.applyMatchResultList(playerRatingAdjustmentMap, Arrays.asList(matchResult1, matchResult2));

        final PlayerRatingAdjustment spongeBobNewRating = newRatingMap.get(spongeBobId);
        final PlayerRatingAdjustment patrickNewRating = newRatingMap.get(patrickId);

        assertThat(spongeBobNewRating.getFinalRating(), is(1016));
        assertThat(patrickNewRating.getFinalRating(), is(1084));
    }

    @Test
    public void testGetPlayerSet() throws Exception {
        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final Set<String> playerSet = ratingManager.getPlayerSet(tournamentResultRequest.getTournamentResultList());

        assertThat(playerSet, containsInAnyOrder(spongeBobUserName, patrickUserName));
    }

    @Test
    public void testGetPlayerRatingAdjustmentMap() throws Exception {
        final int spongeBobRating = 1000;
        final int patrickRating = 1100;

        initializeSpongeBobAndPatrick(spongeBobRating, patrickRating);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final Set<String> playerSet = ratingManager.getPlayerSet(tournamentResultRequest.getTournamentResultList());
        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                ratingManager.getPlayerRatingAdjustmentMap(playerSet);

        final PlayerRatingAdjustment spongeBobRatingAdjustment = playerRatingAdjustmentMap.get(spongeBobUserName);
        final PlayerRatingAdjustment patrickRatingAdjustment = playerRatingAdjustmentMap.get(patrickUserName);

        assertThat(spongeBobRatingAdjustment, notNullValue());
        assertThat(spongeBobRatingAdjustment.getFinalRating(), is(spongeBobRating));
        assertThat(patrickRatingAdjustment, notNullValue());
        assertThat(patrickRatingAdjustment.getFinalRating(), is(patrickRating));
    }

    @Test
    public void testInitializeRatingAndSubmitTournamentResult() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n" +
                "patrick,        1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, true);

        assertThat(ratingAdjustmentResponse.getTournamentName(), is(tournamentName1));
        assertThat(ratingAdjustmentResponse.getRatingAdjustmentResponseList().get(0).getProcessed(), is(true));
        assertThat(ratingAdjustmentResponse.getRatingAdjustmentResponseList().get(1).getProcessed(), is(true));

        final Integer spongeBobId = ratingManager.getPlayerId(spongeBobUserName);
        final Integer patrickId = ratingManager.getPlayerId(patrickUserName);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();
        tournamentResultRequest.setTournamentName(tournamentName2);
        tournamentResultRequest.setTournamentDate(df.parse(tournamentDate2));

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, false);

        assertThat(tournamentResultResponse.getTournamentName(), is(tournamentName2));
        assertThat(tournamentResultResponse.getTournamentDate(), is(df.parse(tournamentDate2)));
        assertThat(tournamentResultResponse.getTournamentId(), notNullValue());

        final List<TournamentResultResponseLineItem> tournamentResultResponseList =
                tournamentResultResponse.getTournamentResultResponseList();

        // verify tournamentResultResponseList, which contains original request plus match result
        assertThat(tournamentResultResponseList, hasSize(1));
        assertThat(tournamentResultResponseList.get(0).getOriginalTournamentResultLineItem().getWinner(),
                is(spongeBobUserName));
        assertThat(tournamentResultResponseList.get(0).getMatchResult().getWinnerRatingDelta(), is(8));

        // verify rating adjustments
        final List<PlayerRatingAdjustment> ratingList = tournamentResultResponse.getRatingAdjustmentList();
        assertThat(ratingList, hasSize(2));
        assertThat(ratingList.get(0).getPlayerId(), anyOf(is(spongeBobId), is(patrickId)));
        assertThat(ratingList.get(0).getInitialRating(), is(1000));
        assertThat(ratingList.get(0).getFirstPassRating(), is(1000));
        assertThat(ratingList.get(0).getFinalRating(), anyOf(is(1008), is(992)));
        assertThat(ratingList.get(1).getPlayerId(), anyOf(is(spongeBobId), is(patrickId)));
        assertThat(ratingList.get(1).getInitialRating(), is(1000));
        assertThat(ratingList.get(1).getFirstPassRating(), is(1000));
        assertThat(ratingList.get(1).getFinalRating(), anyOf(is(1008), is(992)));

        final PlayerRatingAdjustment spongBobRatingAdjustment = ratingManager.getRating(spongeBobId).orElseThrow(
                () -> new Exception("Could not get SpongeBob rating")
        );
        assertThat(spongBobRatingAdjustment.getInitialRating(), is(1000));
        assertThat(spongBobRatingAdjustment.getFirstPassRating(), is(1000));
        assertThat(spongBobRatingAdjustment.getFinalRating(), is(1008));

        final PlayerRatingAdjustment patrickRatingAdjustment = ratingManager.getRating(patrickId).orElseThrow(
                () -> new Exception("Could not get Patrick rating")
        );
        assertThat(patrickRatingAdjustment.getInitialRating(), is(1000));
        assertThat(patrickRatingAdjustment.getFirstPassRating(), is(1000));
        assertThat(patrickRatingAdjustment.getFinalRating(), is(992));
    }

    @Test(expected = DuplicateTournamentException.class)
    public void testSubmitTournamentResultDuplicateError() throws Exception {
        initializeSpongeBobAndPatrick(0, 0);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();
        tournamentResultRequest.setTournamentName(tournamentName2);
        tournamentResultRequest.setTournamentDate(df.parse(tournamentDate2));

        ratingManager.submitTournamentResult(tournamentResultRequest, false);
        ratingManager.submitTournamentResult(tournamentResultRequest, false);
    }

    @Test
    public void testSubmitTournamentResultInvalidWinner() throws Exception {
        ratingManager.addPlayer(patrick);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, false);

        final TournamentResultResponseLineItem responseLineItem =
                tournamentResultResponse.getTournamentResultResponseList().get(0);

        assertFalse(tournamentResultResponse.isProcessed());
        assertFalse(responseLineItem.isProcessed());
        assertThat(responseLineItem.getRejectReason(),
                is(TournamentResultResponseLineItem.REJECT_REASON_INVALID_WINNER));
        assertThat(responseLineItem.getOriginalTournamentResultLineItem().getWinner(), is(spongeBobUserName));

        // tournament should not be added if we cannot post result
        assertFalse(ratingManager.getTournament(tournamentName2).isPresent());
    }

    @Test
    public void testSubmitTournamentResultInvalidLoser() throws Exception {
        ratingManager.addPlayer(spongeBob);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, false);

        final TournamentResultResponseLineItem responseLineItem =
                tournamentResultResponse.getTournamentResultResponseList().get(0);

        assertFalse(tournamentResultResponse.isProcessed());
        assertFalse(responseLineItem.isProcessed());
        assertThat(responseLineItem.getRejectReason(),
                is(TournamentResultResponseLineItem.REJECT_REASON_INVALID_LOSER));
        assertThat(responseLineItem.getOriginalTournamentResultLineItem().getLoser(), is(patrickUserName));

        // tournament should not be added if we cannot post result
        assertFalse(ratingManager.getTournament(tournamentName2).isPresent());
    }

    @Test
    public void testSubmitTournamentResultOnlyProcessIfNoErrors() throws Exception {
        initializeSpongeBobAndPatrick(1000, 1100);

        // patrick and spongebob have valid tournament result
        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        // but these two players do not exist
        final TournamentResultRequestLineItem badLineItem = new TournamentResultRequestLineItem();
        badLineItem.setWinner("bad winner");
        badLineItem.setLoser("bad loser");

        // add invalid tournamentResultRequestLineItem to original request
        tournamentResultRequest.setTournamentResultList(new TournamentResultRequestLineItem[] {
                tournamentResultRequest.getTournamentResultList()[0], // from original good request
                badLineItem // bad request
        });

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, false);

        // for clarity, the good request does not get returned
        // the bad request gets returned at position 0
        final TournamentResultResponseLineItem responseLineItem =
                tournamentResultResponse.getTournamentResultResponseList().get(0);

        assertFalse(tournamentResultResponse.isProcessed());
        assertFalse(responseLineItem.isProcessed());
        assertThat(responseLineItem.getRejectReason(),
                is(TournamentResultResponseLineItem.REJECT_REASON_INVALID_WINNER));

        // tournament should not be added if we cannot post result
        assertFalse(ratingManager.getTournament(tournamentName2).isPresent());
    }

    @Test
    @Ignore
    public void testSubmitTournamentResultPlayerExistsButHasNoRating() throws Exception {
        ratingManager.addPlayer(spongeBob);
        ratingManager.addPlayer(patrick);

        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, false);

        final TournamentResultResponseLineItem responseLineItem =
                tournamentResultResponse.getTournamentResultResponseList().get(0);

        assertFalse(responseLineItem.isProcessed());
        assertThat(responseLineItem.getRejectReason(),
                is(TournamentResultResponseLineItem.REJECT_REASON_INVALID_LOSER));
        assertThat(responseLineItem.getOriginalTournamentResultLineItem().getLoser(), is(patrickUserName));
    }

    @Test
    @Ignore
    public void testSubmitTournamentResultAutoAddPlayer() throws Exception {
        final TournamentResultRequest tournamentResultRequest =
                createTournamentResultRequestForSpongeBobAndPatrick();

        final TournamentResultResponse tournamentResultResponse =
                ratingManager.submitTournamentResult(tournamentResultRequest, true);

        final TournamentResultResponseLineItem responseLineItem =
                tournamentResultResponse.getTournamentResultResponseList().get(0);

        assertTrue(responseLineItem.isProcessed());
    }
}