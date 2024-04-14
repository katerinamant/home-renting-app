package com.homerentals.backend;

public enum Requests {
    // Host Requests
    NEW_RENTAL,
    UPDATE_AVAILABILITY,
    GET_BOOKINGS,

    // Guest Requests
    GET_RENTALS,
    NEW_BOOKING,
    NEW_RATING,
    GET_BOOKINGS_WITH_NO_RATINGS,

    // Miscellaneous Requests
    CLOSE_CONNECTION,
}
