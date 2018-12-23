package com.eatsleeppong.ubipong.manager;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestRatingManager {
    private RatingManager ratingManager;

    private Integer spongeBobId = 1;
    private Integer patrickId = 2;
    private Integer squidwardId = 3;

    private Player spongebob;
    private Player patrick;
    private Player squidward;

    @Before
    public void setup() {
        Player spongebob = new Player();
        spongebob.setPlayerId(spongeBobId);

        Player patrick = new Player();
        patrick.setPlayerId(patrickId);

        Player squidward = new Player();
        squidward.setPlayerId(squidwardId);

        ratingManager = new RatingManager();
    }

    @Test
    public void canary() {

    }

    @Test
    public void getPlayerRating() {
        final Integer finalRating = 1200;

        PlayerRatingAdjustment playerRatingAdjustment = new PlayerRatingAdjustment();

        playerRatingAdjustment.setPlayerRatingAdjustmentId(100);
        playerRatingAdjustment.setPlayerId(spongeBobId);
        playerRatingAdjustment.setInitialRating(1000);
        playerRatingAdjustment.setFirstPassRating(1100);
        playerRatingAdjustment.setFinalRating(finalRating);

        ratingManager.adjustRating(playerRatingAdjustment);

        PlayerRatingAdjustment ratingAdjustment = ratingManager.getRating(spongeBobId);

        assertThat(ratingAdjustment.getFinalRating(), is(finalRating));
    }
}
