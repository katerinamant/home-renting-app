package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class PasswordTest {
    @Test
    public void set_values() {
        Password password = new Password();

        password.setPassword("Example1!");
        Assert.assertEquals("Example1!", password.getPassword());
    }

    @Test
    public void constructor_with_args() {
        Password password = new Password("Example1!");

        Assert.assertEquals("Example1!", password.getPassword());
    }

    @Test
    public void is_valid() {
        Assert.assertTrue(Password.isValid("Example1!"));

        Assert.assertFalse(Password.isValid(""));
        Assert.assertFalse(Password.isValid("Examp1!")); // less than 8 characters
        Assert.assertFalse(Password.isValid("example1!")); // no upper case letter
        Assert.assertFalse(Password.isValid("EXAMPLE1!")); // no lower case letter
        Assert.assertFalse(Password.isValid("Example!!")); // no number
        Assert.assertFalse(Password.isValid("Example11")); // no special character
    }
}
