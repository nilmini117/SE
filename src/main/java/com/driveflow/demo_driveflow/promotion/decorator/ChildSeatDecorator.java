package com.driveflow.demo_driveflow.promotion.decorator;

public class ChildSeatDecorator extends RentalCostDecorator {
    
    public ChildSeatDecorator(RentalCost cost) { 
        super(cost); 
    }

    @Override
    public String getDescription() { 
        return decoratedCost.getDescription() + " + Child Safety Seat"; 
    }

    @Override
    public double getCost() { 
        // Adds Rs. 800 (per day) for 3 days
        return decoratedCost.getCost() + 2400.00; 
    }
}
