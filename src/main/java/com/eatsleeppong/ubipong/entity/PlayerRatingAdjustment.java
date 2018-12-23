package com.eatsleeppong.ubipong.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class PlayerRatingAdjustment {
    @Id
    @GeneratedValue
    private Integer playerRatingAdjustmentId;

    @Column(nullable = false)
    private Integer tournamentId;

    @Column(nullable = false)
    private Integer playerId;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date tournamentDate;

    @Column(nullable = false)
    private Integer initialRating;

    @Column(nullable = false)
    private Integer firstPassRating;

    @Column(nullable = false)
    private Integer finalRating;
}
