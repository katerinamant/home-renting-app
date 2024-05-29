package com.homerentals.domain;

import java.io.Serializable;

public class RatingsAggregator implements Serializable {
    private int numOfRatings;
    private int sumOfRatings;

    protected RatingsAggregator(int numOfRatings, int sumOfRatings) {
        this.numOfRatings = numOfRatings;
        this.sumOfRatings = sumOfRatings;
    }

    protected double getAverage() {
        return (this.numOfRatings == 0) ? 0 : Math.floor((double) this.sumOfRatings / this.numOfRatings * 100) / 100;
    }

    protected int getNumOfRatings() {
        return this.numOfRatings;
    }

    protected double getStars() {
        // Rounds the average
        // to the nearest half
        return ((int) (this.getAverage() * 2 + 0.5)) / 2.0;
    }

    protected void addRating(int rating) {
        this.numOfRatings += 1;
        this.sumOfRatings += rating;
    }
}
