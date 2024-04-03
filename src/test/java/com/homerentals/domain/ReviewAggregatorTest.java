package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class ReviewAggregatorTest {
    @Test
    public void test_reviews() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens", 50.0, 2, 5, 20, "01/01/2024", "31/12/2025", "\\path");

        ReviewAggregator reviewAggregator = rental.getReviewAggregator();
        Assert.assertTrue(4 == reviewAggregator.getAverage());
        Assert.assertTrue(4 == reviewAggregator.getStars());

        reviewAggregator.addReview(2);
        Assert.assertTrue(3.66 == reviewAggregator.getAverage());
        Assert.assertTrue(3.5 == reviewAggregator.getStars());
    }
}
