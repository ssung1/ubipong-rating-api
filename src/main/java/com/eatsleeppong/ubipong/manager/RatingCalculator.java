package com.eatsleeppong.ubipong.manager;

import org.springframework.stereotype.Service;

@Service
public class RatingCalculator {
    public int calculateWinnerDelta(final int winnerRating, final int loserRating) {
        int diff = winnerRating - loserRating;

        // expected result: when winner has higher rating than loser
        if (diff >= 238) {
            return 0;
        } else if (diff >= 188) {
            return 1;
        } else if (diff >= 138 ) {
            return 2;
        } else if (diff >= 113 ) {
            return 3;
        } else if (diff >= 88) {
            return 4;
        } else if (diff >= 63) {
            return 5;
        } else if (diff >= 38) {
            return 6;
        } else if (diff >= 13) {
            return 7;
        } else if (diff >= 0) {
            return 8;
        }

        // upset result: when winner has lower rating than loser
        else if (diff <= -238) {
            return 50;
        } else if (diff <= -213) {
            return 45;
        } else if (diff <= -188) {
            return 40;
        } else if (diff <= -163) {
            return 35;
        } else if (diff <= -138) {
            return 30;
        } else if (diff <= -113) {
            return 25;
        } else if (diff <= -88) {
            return 20;
        } else if (diff <= -63) {
            return 16;
        } else if (diff <= -38) {
            return 13;
        } else if (diff <= -13) {
            return 10;
        } else {
            return 8;
        }
    }

    public int calculateLoserDelta(final int winnerRating, final int loserRating) {
        return -calculateWinnerDelta(winnerRating, loserRating);
    }
}
