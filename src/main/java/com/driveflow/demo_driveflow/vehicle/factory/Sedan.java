package com.driveflow.demo_driveflow.vehicle.factory;

public class Sedan implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Driving a Sedan");
    }

    @Override
    public void displaySpecifications() {
        System.out.println("Specs: 5 Passengers, Standard AC, City Driving.");
    }
}
