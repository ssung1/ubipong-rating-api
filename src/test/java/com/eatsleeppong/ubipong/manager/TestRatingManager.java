package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.controller.DuplicateTournamentException;
import com.eatsleeppong.ubipong.entity.MatchResult;
import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.entity.Tournament;
import com.eatsleeppong.ubipong.model.PlayerRatingLineItemResult;
import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;
import com.eatsleeppong.ubipong.model.TournamentResultLineItem;
import name.subroutine.etable.CsvTable;
import org.aspectj.apache.bcel.util.Play;
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

        assertThat(ratingManager.adjustRating(spongeBobRating), notNullValue());
        assertThat(ratingManager.adjustRating(patrickRating), notNullValue());

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

    private Map<String, PlayerRatingAdjustment> initializePlayerRatingAdjustmentForSpongeBobAndPatrick(
            final Integer spongeBobInitialRating, final Integer patrickInitialRating) {
        final PlayerRatingAdjustment spongeBobRating = new PlayerRatingAdjustment();
        spongeBobRating.setPlayerId(spongeBobId);
        spongeBobRating.setInitialRating(spongeBobInitialRating);

        final PlayerRatingAdjustment patrickRating = new PlayerRatingAdjustment();
        patrickRating.setPlayerId(patrickId);
        patrickRating.setInitialRating(patrickInitialRating);

        return new HashMap<String, PlayerRatingAdjustment>() {{
                    put(spongeBobUserName, spongeBobRating);
                    put(patrickUserName, patrickRating);
        }};
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
                "tournamentName, test-tournament-1\n" +
                "date, 2019-01-01T00:00:00-0500\n" +
                "player, rating\n" +
                "\"inva\"\"lid\",       Rating\n" +
                "spongebob,      1000\n" +
                "\"patrick\",    1100\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, false);
        final List<PlayerRatingLineItemResult> playerRatingResultList =
                ratingAdjustmentResponse.getPlayerRatingResultList();

        assertFalse(playerRatingResultList.get(0).getProcessed());
        assertThat(playerRatingResultList.get(0).getOriginalRequest().getPlayerUserName(), is("inva\"lid"));
        assertThat(playerRatingResultList.get(0).getOriginalRequest().getRating(), is("Rating"));
        assertTrue(playerRatingResultList.get(1).getProcessed());
        assertThat(playerRatingResultList.get(1).getOriginalRequest().getPlayerUserName(), is("spongebob"));
        assertThat(playerRatingResultList.get(1).getOriginalRequest().getRating(), is("1000"));
        assertTrue(playerRatingResultList.get(2).getProcessed());
        assertThat(playerRatingResultList.get(2).getOriginalRequest().getPlayerUserName(), is("patrick"));
        assertThat(playerRatingResultList.get(2).getOriginalRequest().getRating(), is("1100"));

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
                "tournamentName, test-tournament-1\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      asdf\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, false);
        final List<PlayerRatingLineItemResult> playerRatingResultList =
            ratingAdjustmentResponse.getPlayerRatingResultList();
        final PlayerRatingLineItemResult playerRatingResult = playerRatingResultList.get(0);

        assertFalse(playerRatingResult.getProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(PlayerRatingLineItemResult.REJECT_REASON_INVALID_RATING));
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
        final List<PlayerRatingLineItemResult> playerRatingResultList =
            ratingAdjustmentResponse.getPlayerRatingResultList();
        final PlayerRatingLineItemResult playerRatingResult = playerRatingResultList.get(0);

        assertFalse(playerRatingResult.getProcessed());
        assertThat(playerRatingResult.getRejectReason(),
                is(PlayerRatingLineItemResult.REJECT_REASON_INVALID_PLAYER));
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerWithAutoAdd() throws Exception {
        final String inputString =
                "tournamentName, test-tournament-1\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, true);
        final List<PlayerRatingLineItemResult> playerRatingResultList =
            ratingAdjustmentResponse.getPlayerRatingResultList();
        final PlayerRatingLineItemResult playerRatingResult = playerRatingResultList.get(0);

        assertTrue(playerRatingResult.getProcessed());

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
        final List<PlayerRatingLineItemResult> playerRatingResultList =
            ratingAdjustmentResponse.getPlayerRatingResultList();
        final PlayerRatingLineItemResult playerRatingResult = playerRatingResultList.get(0);

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
    }

    @Test
    public void dryRunWithSummaryOfFailedOnly() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n" +
                "patrick,        \n";

        ratingManager.addPlayer(patrick);

        final RatingAdjustmentResponse ratingAdjustmentResponse = ratingManager.verifyRatingByCsv(inputString);
        final List<PlayerRatingLineItemResult> playerRatingLineItemResultList =
                ratingAdjustmentResponse.getPlayerRatingResultList();

        assertThat(playerRatingLineItemResultList.get(0).getRejectReason(),
                is(PlayerRatingLineItemResult.REJECT_REASON_INVALID_PLAYER));
        assertThat(playerRatingLineItemResultList.get(1).getRejectReason(),
            is(PlayerRatingLineItemResult.REJECT_REASON_INVALID_RATING));
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
                "Player,       Rating\n" +
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
    public void generateMatchResult() {
        final Integer spongeBobInitialRating = 1000;
        final Integer patrickInitialRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                initializePlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobInitialRating, patrickInitialRating);

        final TournamentResultLineItem tournamentResultLineItem = new TournamentResultLineItem();
        tournamentResultLineItem.setWinner(spongeBobUserName);
        tournamentResultLineItem.setLoser(patrickUserName);

//        final Integer spongeBobId = ratingManager.getPlayerId(spongeBobUserName);
//        final Integer patrickId = ratingManager.getPlayerId(patrickUserName);
//
//        final List<PlayerRatingLineItem> playerRatingLineItemList =
//                ratingManager.generatePlayerRatingLineItem(tournamentResultLineItem);
//
//        final PlayerRatingLineItem winnerItem = playerRatingLineItemList.get(0);
//        final PlayerRatingLineItem loserItem = playerRatingLineItemList.get(1);
//
//        assertThat(winnerItem.getPlayerUserName(), is(spongeBobUserName));
//        assertThat(winnerItem.getRating(), is("1020"));
//        assertThat(loserItem.getPlayerUserName(), is(patrickUserName));
//        assertThat(loserItem.getRating(), is("1080"));
        final MatchResult matchResult = ratingManager.generateMatchResult(playerRatingAdjustmentMap,
                tournamentResultLineItem);
        assertThat(matchResult.getWinnerId(), is(spongeBobId));
        assertThat(matchResult.getLoserId(), is(patrickId));
        assertThat(matchResult.getWinnerRatingDelta(), is(20));
    }

    @Test
    public void applyEmptyMatchResultList() {
        final Integer spongeBobInitialRating = 1000;
        final Integer patrickInitialRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
                initializePlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobInitialRating, patrickInitialRating);

        final Map<Integer, PlayerRatingAdjustment> newRatingMap =
                ratingManager.applyMatchResultList(playerRatingAdjustmentMap, Collections.emptyList());

        final PlayerRatingAdjustment spongeBobNewRating = newRatingMap.get(spongeBobId);
        final PlayerRatingAdjustment patrickNewRating = newRatingMap.get(patrickId);

        assertThat(spongeBobNewRating.getFinalRating(), is(spongeBobInitialRating));
        assertThat(patrickNewRating.getFinalRating(), is(patrickInitialRating));
    }

    @Test
    public void applyMatchResultListSize1() {
        final Integer spongeBobInitialRating = 1000;
        final Integer patrickInitialRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
            initializePlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobInitialRating, patrickInitialRating);

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
        final Integer spongeBobInitialRating = 1000;
        final Integer patrickInitialRating = 1100;

        final Map<String, PlayerRatingAdjustment> playerRatingAdjustmentMap =
            initializePlayerRatingAdjustmentForSpongeBobAndPatrick(spongeBobInitialRating, patrickInitialRating);

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
    @Ignore("finish later")
    public void testInitializeRatingAndSubmitTournamentResult() throws Exception {
        final String inputString =
                "tournamentName, " + tournamentName1 + "\n" +
                "date, " + tournamentDate1 + "\n" +
                "player, rating\n" +
                "spongebob,      1000\n" +
                "patrick,        1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
            ratingManager.adjustRatingByCsv(inputString, true);
//        final List<PlayerRatingLineItemResult> playerRatingResultList =
//            ratingAdjustmentResponse.getPlayerRatingResultList();
//        final PlayerRatingLineItemResult playerRatingResult = playerRatingResultList.get(0);
//
//        assertFalse(playerRatingResult.getProcessed());
//        assertThat(playerRatingResult.getRejectReason(),
//            is(PlayerRatingLineItemResult.REJECT_REASON_INVALID_PLAYER));

        assertThat(ratingAdjustmentResponse.getTournamentName(), is(tournamentName1));
        assertThat(ratingAdjustmentResponse.getPlayerRatingResultList().get(0).getProcessed(), is(true));
        assertThat(ratingAdjustmentResponse.getPlayerRatingResultList().get(1).getProcessed(), is(true));


    }
}