package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class RatingsAggregatorTest {
    @Test
    public void test_reviews() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens",
                50.0, 2, 5, 20,  0);

        RatingsAggregator ratingsAggregator = rental.getReviewAggregator();
        Assert.assertEquals(4, ratingsAggregator.getAverage(), 0.0);
        Assert.assertEquals(4, ratingsAggregator.getStars(), 0.0);

        ratingsAggregator.addRating(2);
        Assert.assertEquals(3.66, ratingsAggregator.getAverage(), 0.0);
        Assert.assertEquals(3.5, ratingsAggregator.getStars(), 0.0);
    }
}
