package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_seq")
    private Integer playerId;

    @Column(nullable = false, unique = true)
    private String userName;

    private String email;

    private String firstName;
    private String lastName;
}
