package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

public class RentalTest {
    @Test
    public void constructor_with_args() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens", 50.0, 2, 5, 20, "01/01/2024", "31/12/2025", "\\path");

        Assert.assertEquals(hostAccount, rental.getHost());
        Assert.assertEquals("Rental", rental.getRoomName());
        Assert.assertEquals("Athens", rental.getArea());
        Assert.assertTrue(50.0 == rental.getNightlyRate());
        Assert.assertTrue(2 == rental.getCapacity());
        Assert.assertEquals("\\path", rental.getImagePath());

        Assert.assertTrue(1 == rental.getStartDate().getDayOfMonth());
        Assert.assertTrue(31 == rental.getEndDate().getDayOfMonth());

        Assert.assertTrue(4 == rental.getStars());

        Assert.assertTrue(rental.getAvailability(rental.getStartDate(), rental.getEndDate()));
        rental.toggleAvailability(rental.getStartDate(), rental.getEndDate());
        Assert.assertFalse(rental.getAvailability(rental.getStartDate(), rental.getEndDate()));
    }

    @Test
    public void reviews() {
        HostAccount hostAccount = new HostAccount();
        Rental rental = new Rental(hostAccount, "Rental", "Athens", 50.0, 2, 5, 20, "01/01/2024", "31/12/2025", "\\path");

        rental.addReview(2);
        Assert.assertTrue(3.5 == rental.getStars());
    }
}
