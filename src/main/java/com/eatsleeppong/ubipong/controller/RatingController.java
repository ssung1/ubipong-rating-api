package com.eatsleeppong.ubipong.controller;

import com.eatsleeppong.ubipong.manager.RatingManager;
import com.eatsleeppong.ubipong.model.PlayerRatingLineItemResult;
import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/rest/rating")
public class RatingController {
    private RatingManager ratingManager;

    public RatingController(
        final RatingManager ratingManager
    ) {
        this.ratingManager = ratingManager;
    }

    @PostMapping(value = "/rating-adjustment", consumes = "text/csv", produces = MediaType.APPLICATION_JSON_VALUE)
    public RatingAdjustmentResponse ratingAdjustment(@RequestBody String ratingAdjustmentCsv)
            throws IOException, ParseException {
        return this.ratingManager.adjustRatingByCsv(ratingAdjustmentCsv, true);
    }

    @RequestMapping("/test")
    public String test() {
        return "this is my rating";
    }
}