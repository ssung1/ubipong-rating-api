package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * After each tournament, the player will get one of these records as the overall rating change.  It includes the
 * rating before and after the tournament.
 */
@Data
@Entity
public class PlayerRatingAdjustment implements Cloneable {
    // example of table generator
    // @TableGenerator(
    //     name = "playerRatingAdjustmentGen",
    //     table = "id_gen",
    //     pkColumnName = "gen_key",
    //     valueColumnName = "get_value",
    //     pkColumnValue = "playerRatingAdjustmentId",
    //     allocationSize = 10)
    // @GeneratedValue(strategy = GenerationType.TABLE, generator = "playerRatingAdjustmentGen")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_rating_adjustment_seq")
    private Integer playerRatingAdjustmentId;

    @Column
    private Integer tournamentId;

    @Column(nullable = false)
    private Integer playerId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date adjustmentDate;

    @Column(nullable = false)
    private Integer initialRating;

    @Column(nullable = false)
    private Integer firstPassRating;

    @Column(nullable = false)
    private Integer finalRating;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
