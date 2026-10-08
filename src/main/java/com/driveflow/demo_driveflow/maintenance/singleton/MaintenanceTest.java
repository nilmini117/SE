package com.driveflow.demo_driveflow.maintenance.singleton;

public class MaintenanceTest {
    public static void main(String[] args) {
        
        // Get the singleton instance
        MaintenanceScheduler scheduler1 = MaintenanceScheduler.getInstance();
        MaintenanceScheduler scheduler2 = MaintenanceScheduler.getInstance();
        
        // This will print 'true' because both variables point to the exact same object in memory
        System.out.println("Are both instances the same? " + (scheduler1 == scheduler2));
        
        // Use the scheduler
        scheduler1.scheduleVehicleMaintenance("WP CA-8942", "2026-10-15", "Oil Change");
    }
}
