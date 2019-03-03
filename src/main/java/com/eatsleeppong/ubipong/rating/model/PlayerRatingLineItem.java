package com.eatsleeppong.ubipong.rating.model;

import lombok.Data;

/**
 * As part of RatingAdjustmentRequest, this is the requested new rating of each player.
 *
 * The new rating is the aggregate of all the gains and losses in a tournament.  But since this line item is usually
 * created manually, we are making it as simple as possible.
 */
@Data
public class PlayerRatingLineItem {
    private String playerUserName;
    private String rating;
}
