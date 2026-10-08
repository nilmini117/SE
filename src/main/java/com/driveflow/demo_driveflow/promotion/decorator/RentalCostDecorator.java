package com.driveflow.demo_driveflow.promotion.decorator;

public abstract class RentalCostDecorator implements RentalCost {
    protected RentalCost decoratedCost;

    public RentalCostDecorator(RentalCost cost) {
        this.decoratedCost = cost;
    }

    @Override
    public String getDescription() { 
        return decoratedCost.getDescription(); 
    }
    
    @Override
    public double getCost() { 
        return decoratedCost.getCost(); 
    }
}
