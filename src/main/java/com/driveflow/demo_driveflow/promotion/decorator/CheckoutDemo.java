package com.driveflow.demo_driveflow.promotion.decorator;

public class CheckoutDemo {
    public static void main(String[] args) {
        
        // 1. Start with the base price
        RentalCost totalCost = new BaseRentalCost();
        System.out.println(totalCost.getDescription() + " : Rs. " + totalCost.getCost());

        // 2. Customer adds a Child Seat
        totalCost = new ChildSeatDecorator(totalCost);
        System.out.println(totalCost.getDescription() + " : Rs. " + totalCost.getCost());

        // 3. Customer applies the Promotion Coupon
        totalCost = new CouponDecorator(totalCost);
        System.out.println(totalCost.getDescription() + " : Rs. " + totalCost.getCost());
    }
}
