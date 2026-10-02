package com.driveflow.demo_driveflow.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private com.driveflow.demo_driveflow.booking.BookingRepository bookingRepository;

    @Mock
    private com.driveflow.demo_driveflow.maintenance.MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Vehicle toyota1;
    private Vehicle toyota2;
    private Vehicle toyota3;
    private Vehicle tesla1;

    @BeforeEach
    void setUp() {
        toyota1 = new Vehicle(1L, "WP CA-1020", "Toyota Prius 2024", 1, "Pearl White", 35000, "AVAILABLE", null);
        toyota2 = new Vehicle(2L, "CP KA-3040", "Toyota Axio Hybrid", 1, "Silver", 42000, "AVAILABLE", null);
        toyota3 = new Vehicle(3L, "WP NC-3344", "Toyota RAV4 Prime", 1, "Midnight Blue", 18000, "AVAILABLE", null);
        tesla1 = new Vehicle(4L, "WP TM-3001", "Tesla Model 3 Dual Motor", 1, "Deep Blue", 8000, "AVAILABLE", null);
    }

    @Test
    @DisplayName("Verify retrieving vehicles by brand returns the 3 distinct brand vehicles")
    void getVehiclesByBrand_shouldReturnBrandVehicles() {
        when(vehicleRepository.findByBrand("Toyota")).thenReturn(List.of(toyota1, toyota2, toyota3));

        List<Vehicle> result = vehicleService.getVehiclesByBrand("Toyota");

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(v -> "Toyota".equalsIgnoreCase(v.getBrand())));
        verify(vehicleRepository, times(1)).findByBrand("Toyota");
    }

    @Test
    @DisplayName("Verify retrieving available vehicles by brand filters correctly")
    void getAvailableVehiclesByBrand_shouldReturnAvailableOnly() {
        when(vehicleRepository.findAvailableByBrand("Tesla")).thenReturn(List.of(tesla1));

        List<Vehicle> result = vehicleService.getAvailableVehiclesByBrand("Tesla");

        assertEquals(1, result.size());
        assertEquals("Tesla", result.get(0).getBrand());
        assertEquals("AVAILABLE", result.get(0).getStatus());
        verify(vehicleRepository, times(1)).findAvailableByBrand("Tesla");
    }

    @Test
    @DisplayName("Verify getting all brands returns exactly the 5 specified brands")
    void getAllBrands_shouldReturn5SpecifiedBrands() {
        List<String> brands = vehicleService.getAllBrands();

        assertEquals(5, brands.size());
        assertTrue(brands.containsAll(List.of("Toyota", "Suzuki", "Honda", "Tesla", "Benz")));
    }

    @Test
    @DisplayName("Simulate vehicle registration payload with quantity=5 and status=UNAVAILABLE; verify saved with quantity=1 and status=AVAILABLE")
    void registerVehicle_shouldIgnoreClientStatusAndQuantity_andEnforceDefaults() {
        // Arrange: Client payload with quantity = 5 and status = UNAVAILABLE
        Vehicle clientPayload = new Vehicle();
        clientPayload.setBrand("Toyota");
        clientPayload.setModel("Toyota Land Cruiser Prado");
        clientPayload.setRegNo("WP LC-7799");
        clientPayload.setColor("Pearl White");
        clientPayload.setMileage(1500);
        clientPayload.setStatus("UNAVAILABLE"); // non-default status passed by client
        clientPayload.setQuantity(5);           // non-default quantity passed by client

        // Mock repository save to capture the persisted entity
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Save vehicle through service layer
        Vehicle savedVehicle = vehicleService.registerVehicle(clientPayload);

        // Assert: Database still saves the record with quantity = 1 and status = AVAILABLE
        assertNotNull(savedVehicle);
        assertEquals("AVAILABLE", savedVehicle.getStatus(), "Operational status must be strictly forced to AVAILABLE");
        assertEquals(1, savedVehicle.getQuantity(), "Quantity must be strictly forced to 1");
        assertEquals("Toyota", savedVehicle.getBrand());

        // Verify repository interaction with ArgumentCaptor
        org.mockito.ArgumentCaptor<Vehicle> captor = org.mockito.ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).save(captor.capture());
        Vehicle persisted = captor.getValue();
        assertEquals("AVAILABLE", persisted.getStatus(), "Persisted entity status must be AVAILABLE");
        assertEquals(1, persisted.getQuantity(), "Persisted entity quantity must be 1");
    }

    @Test
    @DisplayName("Simulate DTO payload with brand, quantity=5 and status=UNAVAILABLE; verify saved with quantity=1 and status=AVAILABLE")
    void registerVehicle_viaDto_shouldEnforceDefaults() {
        // Arrange: DTO payload with brand, quantity = 5 and status = UNAVAILABLE
        VehicleRegistrationDto dto = VehicleRegistrationDto.builder()
                .brand("Tesla")
                .model("Model 3 Highland")
                .regNo("WP TM-9090")
                .color("Solid Black")
                .mileage(200)
                .status("UNAVAILABLE")
                .quantity(5)
                .build();

        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Vehicle saved = vehicleService.registerVehicle(dto);

        // Assert
        assertNotNull(saved);
        assertEquals("AVAILABLE", saved.getStatus(), "Status must be forced to AVAILABLE");
        assertEquals(1, saved.getQuantity(), "Quantity must be forced to 1");
        assertEquals("Tesla", saved.getBrand(), "Brand parameter must be preserved");
    }

    @Test
    @DisplayName("Verify getBrandVehicleCounts returns dynamic counts (e.g. 4 Toyota and 2 Tesla)")
    void getBrandVehicleCounts_shouldCalculateAccurateCounts() {
        Vehicle toyota4 = new Vehicle(5L, "WP CA-9999", "Toyota Corolla Cross", 1, "White", 12000, "AVAILABLE", null);
        Vehicle tesla2 = new Vehicle(6L, "WP TM-8888", "Tesla Model Y Performance", 1, "Red", 5000, "AVAILABLE", null);

        // 4 Toyota vehicles, 2 Tesla vehicles
        when(vehicleRepository.findAll()).thenReturn(List.of(toyota1, toyota2, toyota3, toyota4, tesla1, tesla2));

        java.util.Map<String, Long> brandCounts = vehicleService.getBrandVehicleCounts();

        assertNotNull(brandCounts);
        assertEquals(4L, brandCounts.get("Toyota"), "Toyota count should dynamically reflect 4 vehicles");
        assertEquals(2L, brandCounts.get("Tesla"), "Tesla count should dynamically reflect 2 vehicles");
        assertEquals(0L, brandCounts.get("Suzuki"), "Suzuki count should be 0 when none stationed");
        assertEquals(0L, brandCounts.get("Honda"), "Honda count should be 0 when none stationed");
        assertEquals(0L, brandCounts.get("Benz"), "Benz count should be 0 when none stationed");
    }

    @Test
    @DisplayName("Verify vehicle deletion with active/historical bookings implements soft deletion (status=DECOMMISSIONED)")
    void removeVehicle_withBookings_shouldSoftDelete() {
        when(vehicleRepository.findById(1L)).thenReturn(java.util.Optional.of(toyota1));
        when(bookingRepository.existsByVehicle_VehicleId(1L)).thenReturn(true);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleService.removeVehicle(1L);

        assertEquals("DECOMMISSIONED", toyota1.getStatus(), "Vehicle with bookings must be soft-deleted as DECOMMISSIONED");
        verify(vehicleRepository, times(1)).save(toyota1);
        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    @Test
    @DisplayName("Verify vehicle deletion without bookings/maintenance safely deletes record from repository")
    void removeVehicle_withoutBookings_shouldHardDeleteSafely() {
        Vehicle unbookedVehicle = new Vehicle(99L, "WP TEST-99", "Toyota Yaris", 1, "Red", 100, "AVAILABLE", null);
        when(vehicleRepository.findById(99L)).thenReturn(java.util.Optional.of(unbookedVehicle));
        when(bookingRepository.existsByVehicle_VehicleId(99L)).thenReturn(false);
        when(maintenanceRepository.existsByVehicle_VehicleId(99L)).thenReturn(false);

        vehicleService.removeVehicle(99L);

        verify(vehicleRepository, times(1)).delete(unbookedVehicle);
        verify(vehicleRepository, never()).save(unbookedVehicle);
    }
}
