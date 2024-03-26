package com.homerentals.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rental {
    private Host host;
    private final String roomName;
    private final String area;
    private final int numOfPersons;
    private final int numOfReviews;
    private final int sumOfReviews;

    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
    private final Date startDate;
    private final Date endDate;

    private final String imagePath;

    public Rental(String roomName, String area, int noOfPersons, int numOfReviews, int sumOfReviews, String startDate, String endDate, String imagePath) {
        this.roomName = roomName;
        this.area = area;
        this.numOfPersons = noOfPersons;
        this.numOfReviews = numOfReviews;
        this.sumOfReviews = sumOfReviews;
        try {
            this.startDate = df.parse(startDate);
            this.endDate = df.parse(endDate);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
        this.imagePath = imagePath;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public String getRoomName() {
        return roomName;
    }

    public double getStars() {
        return ((this.numOfReviews == 0) ? 0 : (double) this.sumOfReviews / this.numOfReviews);
    }
}
