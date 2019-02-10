package com.eatsleeppong.ubipong.manager;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestRatingCalculator {
    private final RatingCalculator ratingCalculator = new RatingCalculator();

    @Test
    public void calculateWinnerDelta0PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(123, 123);
        assertThat(delta, is(8));
    }

    @Test
    public void calculateWinnerDelta12PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(246, 234);
        assertThat(delta, is(8));
    }

    @Test
    public void calculateWinnerDelta13PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1454, 1441);
        assertThat(delta, is(7));
    }

    @Test
    public void calculateWinnerDelta37PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1454, 1417);
        assertThat(delta, is(7));
    }

    @Test
    public void calculateWinnerDelta38PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1454, 1417);
        assertThat(delta, is(7));
    }

    @Test
    public void calculateWinnerDelta62PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(253, 191);
        assertThat(delta, is(6));
    }

    @Test
    public void calculateWinnerDelta63PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(163, 100);
        assertThat(delta, is(5));
    }

    @Test
    public void calculateWinnerDelta87PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1631, 1544);
        assertThat(delta, is(5));
    }

    @Test
    public void calculateWinnerDelta88PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1654, 1566);
        assertThat(delta, is(4));
    }

    @Test
    public void calculateWinnerDelta112PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(2543, 2431);
        assertThat(delta, is(4));
    }

    @Test
    public void calculateWinnerDelta113PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1972, 1859);
        assertThat(delta, is(3));
    }

    @Test
    public void calculateWinnerDelta137PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1742, 1605);
        assertThat(delta, is(3));
    }

    @Test
    public void calculateWinnerDelta138PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1742, 1604);
        assertThat(delta, is(2));
    }

    @Test
    public void calculateWinnerDelta162PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1000, 838);
        assertThat(delta, is(2));
    }

    @Test
    public void calculateWinnerDelta163PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1787, 1624);
        assertThat(delta, is(2));
    }

    @Test
    public void calculateWinnerDelta187PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(2543, 2356);
        assertThat(delta, is(2));
    }

    @Test
    public void calculateWinnerDelta188PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(2543, 2355);
        assertThat(delta, is(1));
    }

    @Test
    public void calculateWinnerDelta212PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1234, 1022);
        assertThat(delta, is(1));
    }

    @Test
    public void calculateWinnerDelta213PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1234, 1022);
        assertThat(delta, is(1));
    }

    @Test
    public void calculateWinnerDelta237PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1234, 997);
        assertThat(delta, is(1));
    }

    @Test
    public void calculateWinnerDelta238PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1234, 996);
        assertThat(delta, is(0));
    }

    @Test
    public void calculateWinnerDelta999PointsAbove() {
        final int delta = ratingCalculator.calculateWinnerDelta(1976, 977);
        assertThat(delta, is(0));
    }

    @Test
    public void calculateWinnerDelta0Below() {
        final int delta = ratingCalculator.calculateWinnerDelta(454, 454);
        assertThat(delta, is(8));
    }

    @Test
    public void calculateWinnerDelta12Below() {
        final int delta = ratingCalculator.calculateWinnerDelta(454, 454 + 12);
        assertThat(delta, is(8));
    }

    @Test
    public void calculateWinnerDelta13Below() {
        final int delta = ratingCalculator.calculateWinnerDelta(454, 467);
        assertThat(delta, is(10));
    }

    @Test
    public void calculateWinnerDelta163Below() {
        final int delta = ratingCalculator.calculateWinnerDelta(1567, 1730);
        assertThat(delta, is(35));
    }

    @Test
    public void calculateWinnerDelta213Below() {
        final int delta = ratingCalculator.calculateWinnerDelta(2196, 2409);
        assertThat(delta, is(45));
    }

    @Test
    public void calculateLoserDelta213Below() {
        final int delta = ratingCalculator.calculateLoserDelta(2196, 2409);
        assertThat(delta, is(-45));
    }

    @Test
    public void calculateLoserDelta238Below() {
        final int delta = ratingCalculator.calculateLoserDelta(2196, 2196 + 238);
        assertThat(delta, is(-50));
    }
}
