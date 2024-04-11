package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class RentalTest {
    @Test
    public void constructor_with_args() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens",
                50.0, 2, 5, 20, "\\path", 0);

        Assert.assertEquals(hostAccount, rental.getHostAccount());
        Assert.assertEquals("Rental", rental.getRoomName());
        Assert.assertEquals("Athens", rental.getLocation());
        Assert.assertEquals(50.0, rental.getNightlyRate(), 0.0);
        Assert.assertEquals(2, rental.getCapacity());
        Assert.assertEquals("\\path", rental.getImagePath());

        Assert.assertEquals(4, rental.getStars(), 0.0);
    }

    @Test
    public void reviews() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens",
                50.0, 2, 5, 20,"\\path", 0);

        rental.addRating(2);
        Assert.assertEquals(3.5, rental.getStars(), 0.0);
    }
}
