package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class GuestAccountTest {
    @Test
    public void set_values() {
        GuestAccount guestAccount = new GuestAccount();

        guestAccount.setEmail(new Email("example@example.com"));
        Assert.assertEquals("example@example.com", guestAccount.getEmail().toString());

        guestAccount.setPassword(new Password("Example1!"));
        Assert.assertEquals("Example1!", guestAccount.getPassword().getPassword());

        guestAccount.setFirstName("Example");
        Assert.assertEquals("Example", guestAccount.getFirstName());

        guestAccount.setLastName("Example");
        Assert.assertEquals("Example", guestAccount.getLastName());

        guestAccount.setPhoneNumber(new PhoneNumber("2222222222"));
        Assert.assertEquals("2222222222", guestAccount.getPhoneNumber().getPhoneNumber());
    }

    @Test
    public void constructor_with_args() {
        Email email = new Email("example@example.com");
        Password password = new Password("Example1!");
        PhoneNumber phoneNumber = new PhoneNumber("2222222222");
        GuestAccount guestAccount = new GuestAccount(email, password, "Example", "Example", phoneNumber);

        Assert.assertEquals(email.toString(), guestAccount.getEmail().toString());
        Assert.assertEquals(password.getPassword(), guestAccount.getPassword().getPassword());
        Assert.assertEquals("Example", guestAccount.getFirstName());
        Assert.assertEquals("Example", guestAccount.getLastName());
        Assert.assertEquals(phoneNumber.getPhoneNumber(), guestAccount.getPhoneNumber().getPhoneNumber());
    }
}
