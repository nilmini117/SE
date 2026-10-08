package com.driveflow.demo_driveflow.vehicle.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VehicleFactoryTest {

    @Test
    public void testCreateSedan() {
        VehicleFactory factory = new VehicleFactory();
        Vehicle vehicle = factory.createVehicle("SEDAN");

        assertNotNull(vehicle);
        assertTrue(vehicle instanceof Sedan);
        assertDoesNotThrow(vehicle::drive);
        assertDoesNotThrow(vehicle::displaySpecifications);
    }

    @Test
    public void testCreateSUV() {
        VehicleFactory factory = new VehicleFactory();
        Vehicle vehicle = factory.createVehicle("SUV");

        assertNotNull(vehicle);
        assertTrue(vehicle instanceof SUV);
        assertDoesNotThrow(vehicle::drive);
        assertDoesNotThrow(vehicle::displaySpecifications);
    }

    @Test
    public void testCreateUnknownVehicleType() {
        VehicleFactory factory = new VehicleFactory();
        Vehicle vehicle = factory.createVehicle("TRUCK");
        assertNull(vehicle);
    }
}
