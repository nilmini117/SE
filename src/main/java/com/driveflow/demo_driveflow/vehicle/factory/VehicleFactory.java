package com.driveflow.demo_driveflow.vehicle.factory;

public class VehicleFactory {
    
    public Vehicle createVehicle(String type) {
        if (type == null) {
            return null;
        }
        if (type.equalsIgnoreCase("SEDAN")) {
            return new Sedan();
        } else if (type.equalsIgnoreCase("SUV")) {
            return new SUV();
        } else {
            return null; // unknown vehicle type
        }
    }
}
