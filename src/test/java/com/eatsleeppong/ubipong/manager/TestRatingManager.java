package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TestRatingManager {
    private DateFormat df = new SimpleDateFormat("yyyyMMdd");
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

    private Player spongebob;
    private Player patrick;
    private Player squidward;

    private Date adjustmentDate;

    @Before
    public void setup() throws ParseException {
        spongebob = new Player();
        spongebob.setUserName(spongeBobUserName);

        patrick = new Player();
        patrick.setUserName(patrickUserName);

        squidward = new Player();
        squidward.setUserName(squidwardUserName);

        adjustmentDate = df.parse("20181228");
    }

    @Test
    public void canary() {

    }

    @Test
    public void getPlayerRating() {
        final Integer expectedFinalRating = 1200;

        final PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

        playerRatingAdjustment.setPlayerId(spongeBobId);
        playerRatingAdjustment.setAdjustmentDate(adjustmentDate);
        playerRatingAdjustment.setTournamentId(usOpenId);
        playerRatingAdjustment.setInitialRating(1000);
        playerRatingAdjustment.setFirstPassRating(1100);
        playerRatingAdjustment.setFinalRating(expectedFinalRating);

        final PlayerRatingAdjustment saved = ratingManager.adjustRating(playerRatingAdjustment);

        assertThat(saved.getPlayerRatingAdjustmentId(), notNullValue());

        final PlayerRatingAdjustment finalRating = ratingManager.getRating(spongeBobId).orElseThrow(
            () -> new AssertionError("Cannot final rating")
        );

        assertThat(finalRating.getFinalRating(), is(expectedFinalRating));
    }

    @Test
    public void addPlayer() {
        final Player bob = ratingManager.addPlayer(spongebob);

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
        ratingManager.addPlayer(spongebob);

        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);

        assertTrue(spongebob.isPresent());
        assertThat(spongebob.get().getUserName(), is(spongeBobUserName));
        assertThat(spongebob.map(Player::getUserName).orElse(null), is(spongeBobUserName));
    }

    @Test
    public void getPlayerById() {
        final Player spongebob1 = ratingManager.addPlayer(spongebob);

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
        final Integer spongeBobId = ratingManager.addPlayer(spongebob).getPlayerId();
        final Integer patrickId = ratingManager.addPlayer(patrick).getPlayerId();

        final String inputString =
                "\"Pla\"\"yer\",       Rating\n" +
                "spongebob,      1000\n" +
                "\"patrick\",    1100\n";

        final List<RatingAdjustmentResponse> ratingAdjustmentResponseList =
                ratingManager.adjustRatingByCsv(inputString, false);

        // header does not get processed
        assertFalse(ratingAdjustmentResponseList.get(0).getProcessed());
        assertTrue(ratingAdjustmentResponseList.get(1).getProcessed());
        assertTrue(ratingAdjustmentResponseList.get(2).getProcessed());

        final Integer spongeBobRating = ratingManager.getRating(spongeBobId)
                .map(PlayerRatingAdjustment::getFinalRating).orElse(0);
        final Integer patrickRating = ratingManager.getRating(patrickId)
            .map(PlayerRatingAdjustment::getFinalRating).orElse(0);

        assertThat(spongeBobRating, is(1000));
        assertThat(patrickRating, is(1100));
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidRating() throws Exception {
        ratingManager.addPlayer(spongebob).getPlayerId();

        final String inputString = "spongebob,      asdf\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, false).get(0);

        assertFalse(ratingAdjustmentResponse.getProcessed());
        assertThat(ratingAdjustmentResponse.getRejectReason(),
                is(RatingAdjustmentResponse.RELECT_REASON_INVALID_RATING));
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerNoAutoAdd() throws Exception {
        final String inputString = "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, false).get(0);

        assertFalse(ratingAdjustmentResponse.getProcessed());
        assertThat(ratingAdjustmentResponse.getRejectReason(),
                is(RatingAdjustmentResponse.RELECT_REASON_INVALID_PLAYER));
    }

    @Test
    public void adjustPlayerRatingByCsvInvalidPlayerWithAutoAdd() throws Exception {
        final String inputString = "spongebob,      1000\n";

        final RatingAdjustmentResponse ratingAdjustmentResponse =
                ratingManager.adjustRatingByCsv(inputString, true).get(0);

        assertTrue(ratingAdjustmentResponse.getProcessed());

        final Optional<Player> spongebob = ratingManager.getPlayer(spongeBobUserName);

        final Integer spongeBobRating = spongebob
                .flatMap(p -> ratingManager.getRating(p.getPlayerId()))
                .map(PlayerRatingAdjustment::getFinalRating)
                .orElse(0);

        assertThat(spongeBobRating, is(1000));
    }

    @Test
    public void adjustPlayerRatingByCsvCalculateInitialRating() throws Exception {
        final Integer spongeBobId = ratingManager.addPlayer(spongebob).getPlayerId();

        final String tournament1 =
                "Player,       Rating\n" +
                "spongebob,      1000\n";
        final String tournament2 =
                "Player,       Rating\n" +
                "spongebob,      1100\n";

        ratingManager.adjustRatingByCsv(tournament1, false);
        // when adding a second tournament, the initial rating is taken from the final rating of first tournament
        ratingManager.adjustRatingByCsv(tournament2, false);

        List<PlayerRatingAdjustment> ratingHistory = ratingManager.getRatingHistory(spongeBobId, 2);

        assertThat(ratingHistory.get(0).getFinalRating(), is(1100));
        assertThat(ratingHistory.get(0).getFirstPassRating(), is(1000));
        assertThat(ratingHistory.get(0).getInitialRating(), is(1000));

        assertThat(ratingHistory.get(1).getFinalRating(), is(1000));
        assertThat(ratingHistory.get(1).getFirstPassRating(), is(0));
        assertThat(ratingHistory.get(1).getInitialRating(), is(0));
    }
}
