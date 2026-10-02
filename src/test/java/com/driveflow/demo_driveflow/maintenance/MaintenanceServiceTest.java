package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.email.EmailService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MaintenanceServiceImpl maintenanceService;

    private Vehicle vehicle;
    private MaintenanceCompany company;
    private Maintenance maintenance;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setVehicleId(101L);
        vehicle.setModel("Toyota Prius 2024");
        vehicle.setRegNo("WP CA-1020");
        vehicle.setStatus("AVAILABLE");

        company = new MaintenanceCompany();
        company.setCompanyId(501L);
        company.setCompanyName("AutoCare Precision Services");
        company.setEmail("autocare@precisionfleet.com");
        company.setContactNumber("011-2894567");
        company.setSpeciality("Engine & Transmission Overhaul");

        maintenance = new Maintenance();
        maintenance.setVehicle(vehicle);
        maintenance.setMaintenanceCompany(company);
        maintenance.setServiceDate(LocalDate.now());
        maintenance.setApproximatedCost(new BigDecimal("18500.00"));
    }

    @Test
    @DisplayName("Verify scheduling a vehicle automatically updates status to UNAVAILABLE and triggers email notification")
    void scheduleService_shouldSetVehicleStatusToUnavailable_andTriggerEmailNotification() {
        // Arrange
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(vehicle));
        when(maintenanceCompanyRepository.findById(501L)).thenReturn(Optional.of(company));
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Maintenance scheduled = maintenanceService.scheduleService(maintenance);

        // Assert: 1. Vehicle status successfully changes to UNAVAILABLE
        assertEquals("UNAVAILABLE", vehicle.getStatus(), "Vehicle status must automatically switch to UNAVAILABLE");
        assertEquals("UNAVAILABLE", scheduled.getVehicle().getStatus());

        // Assert: 2. Vehicle repository save was called with the updated UNAVAILABLE vehicle
        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).save(vehicleCaptor.capture());
        assertEquals("UNAVAILABLE", vehicleCaptor.getValue().getStatus());

        // Assert: 3. Maintenance record was saved
        verify(maintenanceRepository, times(1)).save(maintenance);

        // Assert: 4. Automated email notification was dispatched to the assigned company
        verify(emailService, times(1)).sendMaintenanceNotificationEmail(
                eq("autocare@precisionfleet.com"),
                eq("AutoCare Precision Services"),
                eq(vehicle.getDisplayName()),
                eq(maintenance.getServiceDate()),
                eq(new BigDecimal("18500.00"))
        );
    }

    @Test
    @DisplayName("Verify scheduling fails when no external maintenance company is selected")
    void scheduleService_shouldFail_whenNoCompanySelected() {
        maintenance.setMaintenanceCompany(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.scheduleService(maintenance);
        });

        assertTrue(ex.getMessage().contains("external maintenance company"));
        verify(vehicleRepository, never()).save(any());
        verify(maintenanceRepository, never()).save(any());
        verify(emailService, never()).sendMaintenanceNotificationEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Verify scheduling fails when approximated cost is missing or negative")
    void scheduleService_shouldFail_whenApproximatedCostInvalid() {
        maintenance.setApproximatedCost(null);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.scheduleService(maintenance);
        });
        assertTrue(ex1.getMessage().contains("approximated cost"));

        maintenance.setApproximatedCost(new BigDecimal("-50.00"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.scheduleService(maintenance);
        });
        assertTrue(ex2.getMessage().contains("approximated cost"));

        verify(vehicleRepository, never()).save(any());
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify scheduling fails when vehicle is missing")
    void scheduleService_shouldFail_whenNoVehicleSelected() {
        maintenance.setVehicle(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.scheduleService(maintenance);
        });
        assertTrue(ex.getMessage().contains("vehicle must be selected"));
    }
}
