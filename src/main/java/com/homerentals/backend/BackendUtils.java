package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

public class BackendUtils {
    public static final String MESSAGE_TYPE = "type";
    public static final String MESSAGE_HEADER = "header";
    public static final String MESSAGE_BODY = "body";

    public static final String MESSAGE_TYPE_REQUEST = "request";
    public static final String MESSAGE_TYPE_RESPONSE = "response";

    public static final String BODY_FIELD_FILTERS = "filters";
    public static final String BODY_FIELD_MAP_ID = "mapId";
    public static final String BODY_FIELD_RENTAL_ID = "rentalId";
    public static final String BODY_FIELD_RENTAL_LIST = "rentalList";
    public static final String BODY_FIELD_START_DATE = "startDate";
    public static final String BODY_FIELD_END_DATE = "endDate";

    public static final int SERVER_PORT = 8080;
    public static final int REDUCER_PORT = 4040;

    public static JSONObject createRequest(String header, String body) {
        JSONObject request = new JSONObject();
        request.put(MESSAGE_TYPE, MESSAGE_TYPE_REQUEST);
        request.put(MESSAGE_HEADER, header);
        if (body.isEmpty()) body = "{}";
        request.put(MESSAGE_BODY, body);
        return request;
    }

    public static Rental jsonToRentalObject(JSONObject input) {
        try {
            // Create Rental object from JSON
            String roomName = input.getString("roomName");
            String location = input.getString("location");
            double pricePerNight = input.getDouble("nightlyRate");
            int numOfPersons = input.getInt("capacity");
            int numOfReviews = input.getInt("numOfReviews");
            int sumOfReviews = input.getInt("sumOfReviews");
            String imagePath = input.getString("imagePath");
            int rentalId = input.getInt("rentalId");
            Rental rental = new Rental(null, roomName, location, pricePerNight,
                    numOfPersons, numOfReviews, sumOfReviews, imagePath, rentalId);
            System.out.println(rental.getId());
            return rental;

        } catch (JSONException e) {
            // String is not valid JSON object
            System.err.println("RequestHandler.jsonToRentalObject(): Error creating Rental object from JSON: " + e);
            return null;
        }
    }

    // TODO: write to worker/reducer socket

    // TODO: read client/worker socket
}
