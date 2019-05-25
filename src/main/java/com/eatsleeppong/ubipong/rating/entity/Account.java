package com.eatsleeppong.ubipong.rating.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "account_seq")
    private Integer accountId;

    @Column(nullable = false, unique = true)
    private String accountName;

    private String email;

    private String firstName;
    private String lastName;
}
