package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(
        indexes = {
                @Index(name = "user_name_index", columnList = "userName", unique = false)
        }
)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "player_seq")
    private Integer playerId;

    @Column(nullable = false, unique = true)
    private String userName;

    private String email;

    private String firstName;
    private String lastName;

    private String affiliation;

    private Integer accountId;
}
