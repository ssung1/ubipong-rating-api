package com.eatsleeppong.ubipong.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
public class Tournament {
    @Id
    @GeneratedValue
    private Integer tournamentId;

    @Column(nullable = false)
    private Integer initialRating;
}
