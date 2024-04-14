package com.homerentals.dao;

import com.homerentals.domain.GuestAccount;

import java.util.ArrayList;
import java.util.HashMap;

public class GuestAccountDAO {
    protected static ArrayList<GuestAccount> guestAccounts = new ArrayList<>();
    protected static HashMap<String, GuestAccount> emailToGuestAccount = new HashMap<>();

    public GuestAccount find(String emailString, String passwordString) {
        if (emailToGuestAccount.containsKey(emailString)) {
            GuestAccount guestAccount = emailToGuestAccount.get(emailString);
            if (guestAccount.getPassword().equals(passwordString)) {
                // Valid email, valid password
                return guestAccount;
            } else {
                // Valid email, invalid username
                return null;
            }
        }
        // Invalid email
        return null;
    }

    public void save(GuestAccount guestAccount) {
        guestAccounts.add(guestAccount);
        emailToGuestAccount.put(guestAccount.getEmail().toString(), guestAccount);
    }
}
