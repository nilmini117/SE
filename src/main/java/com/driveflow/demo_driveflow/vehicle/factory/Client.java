package com.driveflow.demo_driveflow.vehicle.factory;

public class Client {
    public static void main(String[] args) {
        
        // Create the factory
        VehicleFactory factory = new VehicleFactory();

        // Register a Sedan
        Vehicle v1 = factory.createVehicle("SEDAN");
        v1.drive();
        v1.displaySpecifications();

        // Register an SUV
        Vehicle v2 = factory.createVehicle("SUV");
        v2.drive();
        v2.displaySpecifications();
    }
}
