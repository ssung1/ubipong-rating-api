package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "tournament_seq")
    private Integer tournamentId;

    @Column(nullable = false)
    private String name;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date tournamentDate;

    private Integer accountId;
}
