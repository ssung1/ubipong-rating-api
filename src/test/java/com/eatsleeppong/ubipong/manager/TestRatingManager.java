package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
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
        Player spongebob = new Player();
        spongebob.setPlayerId(spongeBobId);
        spongebob.setUserName(spongeBobUserName);

        Player patrick = new Player();
        patrick.setPlayerId(patrickId);
        patrick.setUserName(patrickUserName);

        Player squidward = new Player();
        squidward.setPlayerId(squidwardId);
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

//        PlayerRatingAdjustment ratingAdjustment = ratingManager.getRating(spongeBobId);
//
//        assertThat(ratingAdjustment.getFinalRating(), is(finalRating));
        assertThat(saved.getPlayerRatingAdjustmentId(), notNullValue());

        final PlayerRatingAdjustment finalRating = ratingManager.getRating(spongeBobId).orElseThrow(
            () -> new AssertionError("Cannot final rating")
        );

        assertThat(finalRating.getFinalRating(), is(expectedFinalRating));
    }

    @Test
    public void addPlayer() {
        Player bob = ratingManager.addPlayer(spongebob);

        assertThat(bob.getPlayerId(), notNullValue());
    }

    @Test
    public void getPlayerByUserName() {
        Player bob = ratingManager.addPlayer(spongebob);

        assertThat(bob.getPlayerId(), notNullValue());

        //ratingManager.addPlayer(
    }
}
