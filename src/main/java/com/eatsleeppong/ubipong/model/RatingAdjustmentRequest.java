package com.eatsleeppong.ubipong.model;

import lombok.Data;

@Data
public class RatingAdjustmentRequest {
    private String playerUserName;
    private String rating;
}
