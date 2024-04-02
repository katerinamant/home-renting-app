package com.homerentals.domain;

public class ReviewAggregator {
    private int numOfReviews;
    private int sumOfReviews;

    protected ReviewAggregator(int numOfReviews, int sumOfReviews) {
        this.numOfReviews = numOfReviews;
        this.sumOfReviews = sumOfReviews;
    }

    protected double getAverage() {
        return ((this.numOfReviews == 0) ? 0 : (double) this.sumOfReviews / this.numOfReviews);
    }

    protected double getStars() {
        // Rounds the average
        // to the nearest half
        return ((int) (this.getAverage()*2 + 0.5)) / 2.0;
    }

    protected void addReview(int review) {
        this.numOfReviews += 1;
        this.sumOfReviews += review;
    }
}
