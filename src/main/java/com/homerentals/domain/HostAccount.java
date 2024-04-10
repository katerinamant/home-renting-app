package com.homerentals.domain;

import java.io.Serializable;

public class HostAccount extends AbstractAccount implements Serializable {
    protected HostAccount() {
        super();
    }

    public HostAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
    }
}
