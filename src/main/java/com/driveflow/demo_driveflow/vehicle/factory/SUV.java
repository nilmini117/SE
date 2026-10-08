package com.driveflow.demo_driveflow.vehicle.factory;

public class SUV implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Driving an SUV");
    }

    @Override
    public void displaySpecifications() {
        System.out.println("Specs: 7 Passengers, 4WD, Off-road capable.");
    }
}
