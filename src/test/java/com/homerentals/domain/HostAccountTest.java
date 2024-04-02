package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class HostAccountTest {
    @Test
    public void set_values() {
        HostAccount hostAccount = new HostAccount();

        hostAccount.setEmail(new Email("example@example.com"));
        Assert.assertEquals("example@example.com", hostAccount.getEmail().toString());

        hostAccount.setPassword(new Password("Example1!"));
        Assert.assertEquals("Example1!", hostAccount.getPassword().getPassword());

        hostAccount.setFirstName("Example");
        Assert.assertEquals("Example", hostAccount.getFirstName());

        hostAccount.setLastName("Example");
        Assert.assertEquals("Example", hostAccount.getLastName());

        hostAccount.setPhoneNumber(new PhoneNumber("2222222222"));
        Assert.assertEquals("2222222222", hostAccount.getPhoneNumber().getPhoneNumber());
    }

    @Test
    public void constructor_with_args() {
        Email email = new Email("example@example.com");
        Password password = new Password("Example1!");
        PhoneNumber phoneNumber = new PhoneNumber("2222222222");
        HostAccount hostAccount = new HostAccount(email, password, "Example", "Example", phoneNumber);

        Assert.assertEquals(email.toString(), hostAccount.getEmail().toString());
        Assert.assertEquals(password.getPassword(), hostAccount.getPassword().getPassword());
        Assert.assertEquals("Example", hostAccount.getFirstName());
        Assert.assertEquals("Example", hostAccount.getLastName());
        Assert.assertEquals(phoneNumber.getPhoneNumber(), hostAccount.getPhoneNumber().getPhoneNumber());
    }
}
