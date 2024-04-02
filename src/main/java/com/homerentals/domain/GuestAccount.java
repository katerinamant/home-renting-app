package com.homerentals.domain;

public class GuestAccount extends AbstractAccount {
    public GuestAccount() { super(); }

    public GuestAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
    }
}
