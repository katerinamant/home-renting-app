package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class PhoneNumberTest {
    @Test
    public void set_values() {
        PhoneNumber phoneNumber = new PhoneNumber();

        phoneNumber.setPhoneNumber("2222222222");
        Assert.assertEquals("2222222222", phoneNumber.getPhoneNumber());
    }

    @Test
    public void constructor_with_args() {
        PhoneNumber phoneNumber = new PhoneNumber("2222222222");

        Assert.assertEquals("2222222222", phoneNumber.getPhoneNumber());
    }

    @Test
    public void is_valid() {
        Assert.assertTrue(PhoneNumber.isValid("2222222222"));

        Assert.assertFalse(PhoneNumber.isValid(null));
        Assert.assertFalse(PhoneNumber.isValid(""));
        Assert.assertFalse(PhoneNumber.isValid("222222")); // less than 10 digits
        Assert.assertFalse(PhoneNumber.isValid("0000000000")); // starts with 0
        Assert.assertFalse(PhoneNumber.isValid("1111111111")); // starts with 1
        Assert.assertFalse(PhoneNumber.isValid("222222222222")); // more than 10 digits
    }
}
