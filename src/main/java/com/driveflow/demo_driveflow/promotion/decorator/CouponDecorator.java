package com.driveflow.demo_driveflow.promotion.decorator;

public class CouponDecorator extends RentalCostDecorator {
    
    public CouponDecorator(RentalCost cost) { 
        super(cost); 
    }

    @Override
    public String getDescription() { 
        return decoratedCost.getDescription() + " + Promo Code [WEB0] (-10%)"; 
    }

    @Override
    public double getCost() { 
        // Applies a 10% discount to whatever cost was passed inside it
        return decoratedCost.getCost() * 0.90; 
    }
}
