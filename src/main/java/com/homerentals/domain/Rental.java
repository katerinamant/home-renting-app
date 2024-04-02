package com.homerentals.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rental {
    private final HostAccount host;
    private final String roomName;
    private final String area;
    private final double pricePerNight;
    private final int numOfPersons;
    private final ReviewAggregator reviews;

    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
    private final Date startDate;
    private final Date endDate;

    private final String imagePath;

    public Rental(HostAccount host, String roomName, String area, double pricePerNight, int noOfPersons, int numOfReviews, int sumOfReviews, String startDate, String endDate, String imagePath) {
        this.host = host;
        this.roomName = roomName;
        this.area = area;
        this.pricePerNight = pricePerNight;
        this.numOfPersons = noOfPersons;
        this.reviews = new ReviewAggregator(numOfReviews, sumOfReviews);
        try {
            this.startDate = df.parse(startDate);
            this.endDate = df.parse(endDate);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
        this.imagePath = imagePath;
    }

    public HostAccount getHost() {
        return this.host;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public String getArea() {
        return this.area;
    }

    public double getPricePerNight() {
        return this.pricePerNight;
    }

    public int getNumOfPersons() {
        return this.numOfPersons;
    }

    public void addReview(int review) {
        this.reviews.addReview(review);
    }

    public double getStars() {
        return this.reviews.getStars();
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public String getImagePath() {
        return this.imagePath;
    }
}
