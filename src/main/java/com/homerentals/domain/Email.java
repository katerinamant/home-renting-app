package com.homerentals.domain;

import java.util.regex.Pattern;

public class Email {
    private String username;
    private String domain;

    public Email(String email) {
        String[] emailArr = email.split("@");
        this.username = emailArr[0];
        this.domain = emailArr[1];
    }

    public static boolean isValid(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pattern = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pattern.matcher(email).matches();
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setEmail(String email) {
        String[] emailArr = email.split("@");
        this.username = emailArr[0];
        this.domain = emailArr[1];
    }

    public String toString() {
        return String.format("%s@%s", this.username, this.domain);
    }
}
