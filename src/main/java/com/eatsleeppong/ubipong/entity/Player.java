package com.eatsleeppong.ubipong.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Player {
    @Id
    @GeneratedValue
    private Integer playerId;

    @Column(nullable = false, unique = true)
    private String userName;

    private String email;

    private String firstName;
    private String lastName;
}
