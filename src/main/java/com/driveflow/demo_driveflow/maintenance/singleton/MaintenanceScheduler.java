package com.driveflow.demo_driveflow.maintenance.singleton;

public class MaintenanceScheduler {
    
    // Step 2: Declare a private static instance variable of the same class
    private static MaintenanceScheduler instance;

    // Step 1: Make the constructor private to prevent other classes from using 'new'
    private MaintenanceScheduler() {
        System.out.println("Central Maintenance Scheduler Initialized.");
    }

    // Step 3: Provide a public static method to get the single instance
    public static MaintenanceScheduler getInstance() {
        // If the instance does not exist yet, create it. Otherwise, return the existing one.
        if (instance == null) {
            instance = new MaintenanceScheduler();
        }
        return instance;
    }

    // Example Business Logic for your Maintenance Module
    public void scheduleVehicleMaintenance(String vehicleRegNo, String date, String issue) {
        System.out.println("Vehicle " + vehicleRegNo + " logged for maintenance on " + date + ". Issue: " + issue);
        // Add your database logic here to save to the maintenance table
    }
}
