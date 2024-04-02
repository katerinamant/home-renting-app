package com.homerentals.domain;

public class HostAccount extends AbstractAccount {
    public HostAccount() { }

    public HostAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
    }
}
