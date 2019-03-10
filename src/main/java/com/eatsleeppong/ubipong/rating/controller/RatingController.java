package com.eatsleeppong.ubipong.rating.controller;

import com.eatsleeppong.ubipong.rating.manager.RatingManager;
import com.eatsleeppong.ubipong.rating.model.RatingAdjustmentResponse;
import com.eatsleeppong.ubipong.rating.model.TournamentResultRequest;
import com.eatsleeppong.ubipong.rating.model.TournamentResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import java.io.IOException;

@RestController
@RequestMapping("/rest/v0/rating")
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
    public ResponseEntity<RatingAdjustmentResponse> postRatingAdjustmentByCsv(
            @RequestBody final String ratingAdjustmentCsv,
            @RequestParam(defaultValue = "false") final boolean autoAddPlayer)
            throws IOException, RatingInputFormatException, DuplicateTournamentException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.ratingManager.adjustRatingByCsv(ratingAdjustmentCsv, autoAddPlayer));
    }

    @PostMapping(value = "/tournament-result", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TournamentResultResponse postTournamentResult(@RequestBody final TournamentResultRequest
            tournamentResultRequest, @RequestParam(defaultValue = "false") final boolean autoAddPlayer)
            throws DuplicateTournamentException {
        return this.ratingManager.submitTournamentResult(tournamentResultRequest, autoAddPlayer);
    }
}