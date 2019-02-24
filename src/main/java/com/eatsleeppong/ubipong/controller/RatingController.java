package com.eatsleeppong.ubipong.controller;

import com.eatsleeppong.ubipong.manager.RatingManager;
import com.eatsleeppong.ubipong.model.RatingAdjustmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/rest/rating")
public class RatingController {
    private RatingManager ratingManager;

    public RatingController(
        final RatingManager ratingManager
    ) {
        this.ratingManager = ratingManager;
    }

    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { RatingInputFormatException.class })
    public Exception ratingInputFormatExceptionHandler(RatingInputFormatException e) throws Exception {
        return e;
    }

    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { DuplicateTournamentException.class })
    public Exception duplicateTournamentExceptionHandler(DuplicateTournamentException e) throws Exception {
        return e;
    }

    @PostMapping(value = "/rating-adjustment", consumes = "text/csv", produces = MediaType.APPLICATION_JSON_VALUE)
    public RatingAdjustmentResponse ratingAdjustmentByCsv(@RequestBody String ratingAdjustmentCsv)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {
        return this.ratingManager.adjustRatingByCsv(ratingAdjustmentCsv, true);
    }
}