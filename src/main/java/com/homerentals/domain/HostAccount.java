package com.homerentals.domain;

public class HostAccount extends AbstractAccount {
    protected HostAccount() {
        super();
    }

    public HostAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
    }
}
