package com.driveflow.demo_driveflow.promotion.decorator;

public class BaseRentalCost implements RentalCost {
    @Override
    public String getDescription() { 
        return "Base Rental Rate"; 
    }
    
    @Override
    public double getCost() { 
        return 135000.00; // Example base rate for 3 days
    }
}
