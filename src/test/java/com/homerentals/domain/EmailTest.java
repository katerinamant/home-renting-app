package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class EmailTest {
    @Test
    public void set_values() {
        Email email = new Email();

        email.setUsername("example");
        Assert.assertEquals("example", email.getUsername());

        email.setDomain("example.com");
        Assert.assertEquals("example.com", email.getDomain());

        email.setEmail("example1@example1.com");
        Assert.assertEquals("example1", email.getUsername());
        Assert.assertEquals("example1.com", email.getDomain());
        Assert.assertEquals("example1@example1.com", email.toString());
    }

    @Test
    public void constructor_with_args() {
        Email email = new Email("example@example.com");

        Assert.assertEquals("example", email.getUsername());
        Assert.assertEquals("example.com", email.getDomain());
    }

    @Test
    public void is_valid() {
        Assert.assertTrue(Email.isValid("example@example.com"));

        Assert.assertFalse(Email.isValid(""));
        Assert.assertFalse(Email.isValid("example"));
        Assert.assertFalse(Email.isValid("example@"));
        Assert.assertFalse(Email.isValid("example@example"));
    }
}
