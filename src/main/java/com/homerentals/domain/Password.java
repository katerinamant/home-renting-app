package com.homerentals.domain;

import java.util.regex.Pattern;

public class Password {
    private String password;

    public Password() {
    }

    public Password(String password) {
        this.password = password;
    }

    /*
    Validates passwords with
    at least 8 characters,
    at least one number,
    at least one upper case letter,
    at least one lower case letter and
    at least one special character.
     */
    public static boolean isValid(String password) {
        String invalidPassRegex = "^(.{0,7}|[^0-9]*|[^A-Z]*|[^a-z]*|[a-zA-Z0-9]*)$";

        Pattern pattern = Pattern.compile(invalidPassRegex);
        if (password == null)
            return false;
        return !pattern.matcher(password).matches();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || String.class != o.getClass()) {
            return false;
        }

        String password = (String) o;
        return password.equals(this.password);
    }
}
