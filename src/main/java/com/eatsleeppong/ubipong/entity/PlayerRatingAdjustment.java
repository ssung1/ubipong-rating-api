package com.eatsleeppong.ubipong.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class PlayerRatingAdjustment {
    @Id
//    @TableGenerator(
//        name = "playerRatingAdjustmentGen",
//        table = "id_gen",
//        pkColumnName = "gen_key",
//        valueColumnName = "get_value",
//        pkColumnValue = "playerRatingAdjustmentId",
//        allocationSize = 10)
//    @GeneratedValue(strategy = GenerationType.TABLE, generator = "playerRatingAdjustmentGen")
    @GeneratedValue
    private Integer playerRatingAdjustmentId;

    @Column(nullable = false)
    private Integer tournamentId;

    @Column(nullable = false)
    private Integer playerId;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date adjustmentDate;

    @Column(nullable = false)
    private Integer initialRating;

    @Column(nullable = false)
    private Integer firstPassRating;

    @Column(nullable = false)
    private Integer finalRating;
}
