package com.homerentals.domain;

import java.util.regex.Pattern;

public class PhoneNumber {
    private String phoneNumber;

    public PhoneNumber() {
    }

    public PhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public static boolean isValid(String phoneNumber) {
        String phoneRegex = "^[2-9]{2}[0-9]{8}$";

        Pattern pattern = Pattern.compile(phoneRegex);
        if (phoneNumber == null)
            return false;
        return pattern.matcher(phoneNumber).matches();
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
