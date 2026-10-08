package com.driveflow.demo_driveflow.maintenance.singleton;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaintenanceSchedulerTest {

    @Test
    public void testSingletonInstanceUniqueness() {
        MaintenanceScheduler s1 = MaintenanceScheduler.getInstance();
        MaintenanceScheduler s2 = MaintenanceScheduler.getInstance();

        assertNotNull(s1);
        assertNotNull(s2);
        assertSame(s1, s2, "Both references should point to the exact same singleton instance.");
    }

    @Test
    public void testScheduleVehicleMaintenanceExecution() {
        MaintenanceScheduler scheduler = MaintenanceScheduler.getInstance();
        assertDoesNotThrow(() -> scheduler.scheduleVehicleMaintenance("WP CA-8942", "2026-10-15", "Oil Change"));
    }
}
