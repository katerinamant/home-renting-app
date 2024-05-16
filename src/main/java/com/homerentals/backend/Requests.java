package com.homerentals.backend;

public enum Requests {
    // Host Requests
    NEW_RENTAL,
    UPDATE_AVAILABILITY,
    GET_ALL_BOOKINGS,
    GET_BOOKINGS_BY_LOCATION,

    // Guest Requests
    CHECK_CREDENTIALS,
    GET_RENTALS,
    NEW_BOOKING,
    NEW_RATING,
    GET_BOOKINGS_WITH_NO_RATINGS,

    // Miscellaneous Requests
    CLOSE_CONNECTION,
}
