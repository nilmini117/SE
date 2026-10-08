package com.driveflow.demo_driveflow.promotion.decorator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RentalCostDecoratorTest {

    @Test
    public void testBaseRentalCost() {
        RentalCost cost = new BaseRentalCost();
        assertEquals("Base Rental Rate", cost.getDescription());
        assertEquals(135000.00, cost.getCost(), 0.001);
    }

    @Test
    public void testChildSeatDecorator() {
        RentalCost cost = new BaseRentalCost();
        cost = new ChildSeatDecorator(cost);
        assertEquals("Base Rental Rate + Child Safety Seat", cost.getDescription());
        assertEquals(137400.00, cost.getCost(), 0.001);
    }

    @Test
    public void testCouponDecorator() {
        RentalCost cost = new BaseRentalCost();
        cost = new CouponDecorator(cost);
        assertTrue(cost.getDescription().contains("Promo Code [WEB0]"));
        assertEquals(121500.00, cost.getCost(), 0.001); // 135000 * 0.90
    }

    @Test
    public void testCombinedDecorators() {
        RentalCost cost = new BaseRentalCost();
        cost = new ChildSeatDecorator(cost);
        cost = new CouponDecorator(cost);

        assertTrue(cost.getDescription().contains("Base Rental Rate"));
        assertTrue(cost.getDescription().contains("Child Safety Seat"));
        assertTrue(cost.getDescription().contains("Promo Code [WEB0]"));
        // (135000 + 2400) * 0.90 = 137400 * 0.90 = 123660.00
        assertEquals(123660.00, cost.getCost(), 0.001);
    }
}
